# Kubernetes Vertical Pod Autoscaler (VPA) - Complete Practical Lab

---

# Objective

In this lab, you will learn how to use the **Vertical Pod Autoscaler (VPA)** to automatically recommend and manage CPU and Memory resources for Kubernetes Pods.

By the end of this lab, you will understand:

- Installing VPA
- VPA Architecture
- VPA Components
- VPA Working
- Recommendation Engine
- Off Mode
- Initial Mode
- Auto Mode
- Resource Recommendations
- Pod Recreation
- Debugging VPA
- Production Best Practices

---

# What is Vertical Pod Autoscaler?

Horizontal Pod Autoscaler (HPA)

- Increases or decreases the number of Pods.

Vertical Pod Autoscaler (VPA)

- Increases or decreases CPU and Memory allocated to a Pod.

Example

```
Spring Boot

Current

CPU = 500m
RAM = 512Mi

↓

Application Load Increased

↓

VPA Recommendation

CPU = 1
RAM = 2Gi
```

Instead of creating more Pods, VPA makes each Pod larger.

---

# VPA Architecture

```
                    Kubernetes Cluster

                           │
                    Metrics Server
                           │
                    Resource Metrics
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
 Recommender          Updater        Admission Controller
        │                  │                  │
        └──────────────┬──────────────────────┘
                       │
                       ▼
              VerticalPodAutoscaler CR
                       │
                       ▼
                  Deployment
                       │
                       ▼
                     Pods
```

---

# VPA Components

## 1. Recommender

Responsible for:

- Reading CPU usage
- Reading Memory usage
- Analyzing historical metrics
- Calculating optimal resources

Output

```
CPU Recommendation

Memory Recommendation
```

---

## 2. Updater

Responsible for

- Comparing current Pod resources with recommendations
- Deciding whether Pods should be recreated

Only used in Auto Mode.

---

## 3. Admission Controller

Runs whenever a new Pod is created.

Responsible for

- Modifying Pod resource requests
- Injecting recommended CPU
- Injecting recommended Memory

---

# Directory Structure

```
vpa-lab/

│
├── 01-deployment.yaml
├── 02-service.yaml
├── 03-vpa-off.yaml
├── 04-vpa-initial.yaml
├── 05-vpa-auto.yaml
└── README.md
```

---

# Prerequisites

- Kubernetes Cluster
- kubectl
- Metrics Server Installed

Verify Metrics Server

```bash
kubectl top nodes

kubectl top pods
```

If metrics are displayed, VPA can collect usage information.

---

# Step 1 : Install VPA

Clone Kubernetes Autoscaler repository

```bash
git clone https://github.com/kubernetes/autoscaler.git
```

Go to VPA directory

```bash
cd autoscaler/vertical-pod-autoscaler
```

Install VPA

```bash
./hack/vpa-up.sh
```

---

# What gets installed?

- Recommender
- Updater
- Admission Controller
- CRDs

---

# Step 2 : Verify Installation

Check Pods

```bash
kubectl get pods -n kube-system
```

Expected

```
vpa-admission-controller

vpa-updater

vpa-recommender
```

Verify CRD

```bash
kubectl get crd | grep verticalpodautoscalers
```

Expected

```
verticalpodautoscalers.autoscaling.k8s.io
```

---

# Step 3 : Deploy Sample Application

## 01-deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment

metadata:
  name: nginx-vpa

spec:
  replicas: 1

  selector:
    matchLabels:
      app: nginx-vpa

  template:

    metadata:
      labels:
        app: nginx-vpa

    spec:

      containers:

      - name: nginx

        image: nginx:latest

        ports:
        - containerPort: 80

        resources:

          requests:
            cpu: 100m
            memory: 128Mi

          limits:
            cpu: 500m
            memory: 512Mi
```

Deploy

```bash
kubectl apply -f 01-deployment.yaml
```

Verify

```bash
kubectl get pods
```

---

# Step 4 : Create Service

## 02-service.yaml

```yaml
apiVersion: v1
kind: Service

metadata:
  name: nginx-vpa-service

spec:

  selector:
    app: nginx-vpa

  ports:

  - port: 80
    targetPort: 80

  type: ClusterIP
```

Deploy

```bash
kubectl apply -f 02-service.yaml
```

---

# Step 5 : Create VPA (Recommendation Only)

## 03-vpa-off.yaml

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler

metadata:
  name: nginx-vpa

spec:

  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: nginx-vpa

  updatePolicy:
    updateMode: Off

  resourcePolicy:

    containerPolicies:

    - containerName: "*"

      minAllowed:

        cpu: 100m
        memory: 128Mi

      maxAllowed:

        cpu: "2"
        memory: 2Gi
```

Deploy

```bash
kubectl apply -f 03-vpa-off.yaml
```

---

# Verify

```bash
kubectl get vpa
```

Expected

```
NAME

MODE

nginx-vpa

Off
```

---

# Step 6 : Generate Load

Create BusyBox Pod

```bash
kubectl run load-generator \
--image=busybox \
--restart=Never \
-- /bin/sh
```

Inside BusyBox

```bash
while true
do
wget -q -O- http://nginx-vpa-service
done
```

Let it run for several minutes.

---

# Step 7 : Observe Recommendations

```bash
kubectl describe vpa nginx-vpa
```

Example

```
Recommendation

Container Name : nginx

Target

CPU : 350m

Memory : 220Mi

Lower Bound

CPU : 200m

Memory : 180Mi

Upper Bound

CPU : 600m

Memory : 400Mi
```

---

# Understanding Recommendations

## Lower Bound

Minimum safe resources.

Example

```
CPU

200m
```

Going below this may impact performance.

---

## Target

Recommended resources.

Example

```
CPU

350m
```

This is what VPA considers optimal.

---

## Upper Bound

Maximum recommended resources.

Example

```
CPU

600m
```

Beyond this would likely waste resources.

---

# Experiment 1 : Off Mode

Current Pod

```
CPU

100m

Memory

128Mi
```

Recommendation

```
CPU

350m

Memory

220Mi
```

Describe Pod

```bash
kubectl describe pod <pod-name>
```

Observation

```
Resources remain unchanged.
```

Reason

Off Mode only generates recommendations.

---

# Experiment 2 : Initial Mode

Delete previous VPA

```bash
kubectl delete vpa nginx-vpa
```

Create

```
04-vpa-initial.yaml
```

Change only

```yaml
updatePolicy:

  updateMode: Initial
```

Apply

```bash
kubectl apply -f 04-vpa-initial.yaml
```

Delete existing Pod

```bash
kubectl delete pod <pod-name>
```

Deployment creates a new Pod.

Observation

```
New Pod starts with recommended resources.
```

Existing running Pods are never updated automatically.

---

# Experiment 3 : Auto Mode

Delete VPA

```bash
kubectl delete vpa nginx-vpa
```

Create

```
05-vpa-auto.yaml
```

```yaml
updatePolicy:

  updateMode: Auto
```

Apply

```bash
kubectl apply -f 05-vpa-auto.yaml
```

Generate load again.

Observe

```
Recommendation Generated

↓

Updater Evicts Pod

↓

Deployment Creates New Pod

↓

Admission Controller Injects New Resources

↓

New Pod Running
```

---

# Verify Updated Resources

Describe Pod

```bash
kubectl describe pod <pod-name>
```

Expected

```
Requests

CPU

350m

Memory

220Mi
```

---

# Useful Commands

## View Metrics

```bash
kubectl top pods
```

```bash
kubectl top nodes
```

---

## View VPA

```bash
kubectl get vpa
```

---

## Describe VPA

```bash
kubectl describe vpa nginx-vpa
```

---

## View Pod

```bash
kubectl describe pod <pod-name>
```

---

## Watch Pods

```bash
watch kubectl get pods
```

---

## Logs

Recommender

```bash
kubectl logs \
-n kube-system \
deployment/vpa-recommender
```

Updater

```bash
kubectl logs \
-n kube-system \
deployment/vpa-updater
```

Admission Controller

```bash
kubectl logs \
-n kube-system \
deployment/vpa-admission-controller
```

---

# VPA Modes Comparison

| Mode | Recommendation | Automatically Updates Existing Pods |
|-------|---------------|--------------------------------------|
| Off | ✅ | ❌ |
| Initial | ✅ | ❌ (only new Pods) |
| Auto | ✅ | ✅ |

---

# HPA vs VPA

| Feature | HPA | VPA |
|----------|-----|-----|
| Adds Pods | ✅ | ❌ |
| Removes Pods | ✅ | ❌ |
| Increases CPU | ❌ | ✅ |
| Increases Memory | ❌ | ✅ |
| Uses Metrics Server | ✅ | ✅ |
| Restarts Pods | ❌ | ✅ (Auto Mode) |

---

# VPA Workflow

```
Application Running

↓

Metrics Server Collects CPU & Memory

↓

Recommender Analyzes Usage

↓

Recommendation Generated

↓

Updater Checks Difference

↓

Auto Mode?

↓

Yes

↓

Evict Old Pod

↓

Deployment Creates New Pod

↓

Admission Controller Injects Recommended Resources

↓

Application Running with Updated Requests
```

---

# Production Best Practices

- Install Metrics Server before using VPA.
- Start with **Off** mode to validate recommendations.
- Use **Initial** mode for workloads that are frequently recreated.
- Use **Auto** mode only after testing and ensuring the application tolerates Pod restarts.
- Define sensible `minAllowed` and `maxAllowed` values to prevent unrealistic recommendations.
- Monitor VPA recommendations over time before applying them automatically.

---

# Common Issues

## No recommendations

Possible causes:

- Metrics Server not installed
- Application has not generated enough workload
- VPA components are not running

Check:

```bash
kubectl top pods
kubectl get pods -n kube-system
```

---

## Pods are not restarted

Possible causes:

- VPA is in `Off` or `Initial` mode
- Recommendations are too close to current requests

Check:

```bash
kubectl describe vpa nginx-vpa
```

---

## Recommendation exceeds desired limits

Ensure `resourcePolicy` defines appropriate `minAllowed` and `maxAllowed` values.

---

# Interview Questions

### What is Vertical Pod Autoscaler?

VPA automatically recommends and adjusts CPU and Memory requests for Pods based on observed resource usage.

---

### Which components are installed with VPA?

- Recommender
- Updater
- Admission Controller

---

### Which VPA mode only generates recommendations?

**Off** mode.

---

### Which VPA mode updates only newly created Pods?

**Initial** mode.

---

### Which VPA mode automatically recreates Pods with new resource requests?

**Auto** mode.

---

### Does VPA require Metrics Server?

Yes. VPA relies on resource metrics to calculate recommendations.

---

# Summary

In this lab you learned:

- ✅ VPA architecture
- ✅ Installing VPA
- ✅ Deploying an application
- ✅ Creating a VerticalPodAutoscaler
- ✅ Reading recommendations
- ✅ Off mode
- ✅ Initial mode
- ✅ Auto mode
- ✅ Pod recreation
- ✅ Debugging VPA
- ✅ Production best practices

---

# Next Topic

The next autoscaling topic is **Cluster Autoscaler**, where you'll learn how Kubernetes automatically adds or removes worker nodes when existing nodes cannot accommodate new Pods created by HPA.