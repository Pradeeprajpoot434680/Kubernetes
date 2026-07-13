# Kubernetes Affinity Lab (Node Affinity, Pod Affinity & Pod Anti-Affinity)

## Objective

In this lab, you will learn how Kubernetes schedules Pods using:

- Node Affinity
- Pod Affinity
- Pod Anti-Affinity

By the end of this lab, you will be able to:

- Create a multi-node Kubernetes cluster using Kind
- Apply labels to worker nodes
- Schedule Pods using **Node Affinity**
- Schedule Pods close to other Pods using **Pod Affinity**
- Distribute Pods across different nodes using **Pod Anti-Affinity**
- Verify scheduling decisions using `kubectl`

---

# Lab Architecture

```
                    Kubernetes Cluster

                 +----------------------+
                 |   Control Plane      |
                 +----------------------+

        +----------------+   +----------------+   +----------------+
        |    Worker-1    |   |    Worker-2    |   |    Worker-3    |
        | disk=ssd       |   | disk=hdd       |   | disk=ssd       |
        | gpu=true       |   | backend=true   |   | frontend=true  |
        +----------------+   +----------------+   +----------------+
```

---

# Directory Structure

```
affinity-lab/
│
├── kind-config.yaml
├── node-affinity-required.yaml
├── node-affinity-preferred.yaml
├── backend.yaml
├── frontend-affinity.yaml
├── frontend-anti-affinity.yaml
└── cleanup.sh
```

---

# Prerequisites

Verify Kind

```bash
kind version
```

Verify kubectl

```bash
kubectl version --client
```

---

# Step 1 : Create a Multi Node Cluster

Create

```
kind-config.yaml
```

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4

nodes:
- role: control-plane
- role: worker
- role: worker
- role: worker
```

Create cluster

```bash
kind create cluster \
--name affinity-demo \
--config kind-config.yaml
```

---

# Step 2 : Verify Cluster

```bash
kubectl get nodes
```

Example

```
NAME
affinity-demo-control-plane
affinity-demo-worker
affinity-demo-worker2
affinity-demo-worker3
```

---

# Step 3 : Add Labels to Nodes

## Worker-1

```bash
kubectl label node affinity-demo-worker disk=ssd
kubectl label node affinity-demo-worker gpu=true
```

---

## Worker-2

```bash
kubectl label node affinity-demo-worker2 disk=hdd
kubectl label node affinity-demo-worker2 backend=true
```

---

## Worker-3

```bash
kubectl label node affinity-demo-worker3 disk=ssd
kubectl label node affinity-demo-worker3 frontend=true
```

---

## Verify Labels

```bash
kubectl get nodes --show-labels
```

---

# ======================================================

# PART-1 : NODE AFFINITY

# ======================================================

---

## Required Node Affinity

Create

```
node-affinity-required.yaml
```

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: node-affinity-required

spec:

  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: disk
            operator: In
            values:
            - ssd

  containers:
  - name: nginx
    image: nginx
```

Deploy

```bash
kubectl apply -f node-affinity-required.yaml
```

Verify

```bash
kubectl get pods -o wide
```

Expected

```
node-affinity-required

↓

Worker-1

OR

Worker-3
```

because both nodes have

```
disk=ssd
```

---

## Describe Pod

```bash
kubectl describe pod node-affinity-required
```

Observe

```
Node:

affinity-demo-worker
```

---

# Preferred Node Affinity

Delete previous pod

```bash
kubectl delete pod node-affinity-required
```

Create

```
node-affinity-preferred.yaml
```

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: node-affinity-preferred

spec:

  affinity:

    nodeAffinity:

      preferredDuringSchedulingIgnoredDuringExecution:

      - weight: 100

        preference:

          matchExpressions:

          - key: gpu

            operator: In

            values:

            - "true"

  containers:

  - name: nginx

    image: nginx
```

Deploy

```bash
kubectl apply -f node-affinity-preferred.yaml
```

Verify

```bash
kubectl get pods -o wide
```

Expected

Scheduler prefers Worker-1 because

```
gpu=true
```

---

# Failure Demonstration

Delete pod

```bash
kubectl delete pod node-affinity-preferred
```

Modify YAML

```yaml
matchExpressions:

- key: disk

  operator: In

  values:

  - nvme
```

Deploy

```bash
kubectl apply -f node-affinity-preferred.yaml
```

Verify

```bash
kubectl get pods
```

Output

```
Pending
```

Describe

```bash
kubectl describe pod node-affinity-preferred
```

Observe

```
Node(s) didn't match Pod's node affinity
```

---

# ======================================================

# PART-2 : POD AFFINITY

# ======================================================

---

# Deploy Backend

Create

```
backend.yaml
```

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: backend

  labels:
    app: backend

spec:

  containers:

  - name: nginx

    image: nginx
```

Deploy

```bash
kubectl apply -f backend.yaml
```

Verify

```bash
kubectl get pods -o wide
```

Suppose

```
Backend

↓

Worker-2
```

---

# Deploy Frontend

Create

```
frontend-affinity.yaml
```

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: frontend

spec:

  affinity:

    podAffinity:

      requiredDuringSchedulingIgnoredDuringExecution:

      - labelSelector:

          matchLabels:

            app: backend

        topologyKey: kubernetes.io/hostname

  containers:

  - name: nginx

    image: nginx
```

Deploy

```bash
kubectl apply -f frontend-affinity.yaml
```

Verify

```bash
kubectl get pods -o wide
```

Expected

```
Backend

↓

Worker-2

Frontend

↓

Worker-2
```

Both Pods run on same node.

---

Describe

```bash
kubectl describe pod frontend
```

Observe Scheduler events.

---

# ======================================================

# PART-3 : POD ANTI AFFINITY

# ======================================================

---

Delete previous frontend

```bash
kubectl delete pod frontend
```

Create

```
frontend-anti-affinity.yaml
```

```yaml
apiVersion: apps/v1
kind: Deployment

metadata:
  name: frontend

spec:

  replicas: 3

  selector:
    matchLabels:
      app: frontend

  template:

    metadata:

      labels:
        app: frontend

    spec:

      affinity:

        podAntiAffinity:

          requiredDuringSchedulingIgnoredDuringExecution:

          - labelSelector:

              matchLabels:

                app: frontend

            topologyKey: kubernetes.io/hostname

      containers:

      - name: nginx

        image: nginx
```

Deploy

```bash
kubectl apply -f frontend-anti-affinity.yaml
```

Verify

```bash
kubectl get pods -o wide
```

Expected

```
frontend-xxxxx

↓

Worker-1

frontend-yyyyy

↓

Worker-2

frontend-zzzzz

↓

Worker-3
```

Each replica is placed on a different node.

---

Describe Deployment

```bash
kubectl describe deployment frontend
```

---

Describe one Pod

```bash
kubectl describe pod <pod-name>
```

Observe scheduling decisions.

---

# Verify Everything

View Nodes

```bash
kubectl get nodes --show-labels
```

View Pods

```bash
kubectl get pods -o wide
```

Describe Pod

```bash
kubectl describe pod <pod-name>
```

Describe Node

```bash
kubectl describe node <node-name>
```

---

# Cleanup

Delete Pods

```bash
kubectl delete pod backend
kubectl delete pod node-affinity-required --ignore-not-found
kubectl delete pod node-affinity-preferred --ignore-not-found
```

Delete Deployment

```bash
kubectl delete deployment frontend
```

Delete Cluster

```bash
kind delete cluster --name affinity-demo
```

---

# What You Learned

✅ Creating a Multi-Node Kind Cluster

✅ Labeling Kubernetes Nodes

✅ Required Node Affinity

✅ Preferred Node Affinity

✅ Understanding Match Expressions

✅ Pod Affinity

✅ Pod Anti-Affinity

✅ Scheduler Decision Making

✅ Scheduling Failures

✅ Verifying Scheduling using kubectl

---

# Comparison Table

| Feature | Node Affinity | Pod Affinity | Pod Anti-Affinity |
|----------|---------------|--------------|-------------------|
| Works On | Node Labels | Pod Labels | Pod Labels |
| Purpose | Select specific nodes | Place Pods together | Separate Pods |
| High Availability | ❌ | ❌ | ✅ |
| Performance | Medium | High | High |
| Used For | GPU, SSD, Region | Backend + Cache | Replica Distribution |

---

# Interview Questions

### What is Node Affinity?

An advanced scheduling feature that schedules Pods on nodes matching specified label rules.

---

### Difference between Required and Preferred Node Affinity?

- Required: Pod must satisfy the rule or remain Pending.
- Preferred: Scheduler tries to satisfy the rule but can choose another node if needed.

---

### What is Pod Affinity?

Schedules Pods close to other Pods based on Pod labels.

---

### What is Pod Anti-Affinity?

Prevents Pods with matching labels from being scheduled close together, improving fault tolerance.

---

### What is topologyKey?

Defines the topology boundary used for affinity rules.

Examples:

- `kubernetes.io/hostname` → Same node
- `topology.kubernetes.io/zone` → Same availability zone
- `topology.kubernetes.io/region` → Same region

---

# Next Topic

The next topic is **Resource Requests, Resource Limits, and Quality of Service (QoS)**, where you'll learn:

- CPU Requests
- Memory Requests
- CPU Limits
- Memory Limits
- Scheduler resource allocation
- QoS Classes (Guaranteed, Burstable, BestEffort)
- OOMKilled behavior
- CPU throttling
- Practical demonstrations with resource-constrained Pods