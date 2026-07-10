# Kubernetes Services - ClusterIP & NodePort

## Introduction

In Kubernetes, Pods are **ephemeral** (temporary). Every Pod gets its own IP address, but when a Pod is deleted or recreated, its IP changes.

Example:

```
Pod-1
IP = 10.244.0.5
```

After restarting:

```
Pod-1
IP = 10.244.0.12
```

Applications communicating directly with Pod IPs would break because the IP changes.

To solve this problem, Kubernetes provides **Services**.

A Service provides:

- A stable IP address
- A stable DNS name
- Load balancing across multiple Pods
- Service discovery

---

# Types of Kubernetes Services

Kubernetes provides four Service types.

| Type | Purpose |
|------|----------|
| ClusterIP | Internal communication inside the cluster |
| NodePort | Expose the application on every Kubernetes Node |
| LoadBalancer | Expose application using a Cloud Load Balancer |
| ExternalName | Maps a Service to an external DNS name |

This document focuses on **ClusterIP** and **NodePort**.

---

# Lab Setup

## Step 1 - Create Kind Cluster

```bash
kind create cluster --name services-demo
```

Verify cluster

```bash
kubectl cluster-info

kubectl get nodes
```

Example

```
NAME
services-demo-control-plane
```

---

## Step 2 - Create Deployment

Create a file

```bash
nano deployment.yaml
```

Paste

```yaml
apiVersion: apps/v1

kind: Deployment

metadata:
  name: nginx-deployment

spec:
  replicas: 3

  selector:
    matchLabels:
      app: nginx

  template:

    metadata:
      labels:
        app: nginx

    spec:
      containers:
      - name: nginx
        image: nginx:1.25

        ports:
        - containerPort: 80
```

Apply Deployment

```bash
kubectl apply -f deployment.yaml
```

Verify

```bash
kubectl get deploy

kubectl get pods

kubectl get pods -o wide
```

Example

```
NAME                               READY

nginx-deployment                   3/3

NAME                               IP

nginx-deployment-xxxxx             10.244.0.5
nginx-deployment-yyyyy             10.244.0.6
nginx-deployment-zzzzz             10.244.0.7
```

Notice

Every Pod has its own IP.

Pods can be recreated at any time.

Their IP addresses are not permanent.

---

# ClusterIP Service

## What is ClusterIP?

ClusterIP is the default Service type in Kubernetes.

It creates:

- One virtual IP
- One DNS name
- Internal load balancing

Accessible only inside the Kubernetes cluster.

---

## Architecture

```
          Client Pod

               |

               ▼

      ClusterIP Service

       10.96.50.100

               |

      -------------------

      |        |        |

      ▼        ▼        ▼

    Pod1     Pod2     Pod3
```

The client never communicates directly with Pod IPs.

Instead, it communicates with the ClusterIP.

---

## Step 3 - Create ClusterIP Service

Create file

```bash
nano clusterip.yaml
```

Paste

```yaml
apiVersion: v1

kind: Service

metadata:
  name: nginx-clusterip

spec:

  type: ClusterIP

  selector:
    app: nginx

  ports:

  - port: 80
    targetPort: 80
```

Apply

```bash
kubectl apply -f clusterip.yaml
```

---

## Step 4 - Verify Service

```bash
kubectl get svc
```

Example

```
NAME               TYPE         CLUSTER-IP

nginx-clusterip    ClusterIP    10.96.210.15
```

---

## Step 5 - Describe Service

```bash
kubectl describe svc nginx-clusterip
```

Example

```
Name: nginx-clusterip

Type: ClusterIP

IP: 10.96.210.15

Selector: app=nginx

Endpoints:

10.244.0.5:80

10.244.0.6:80

10.244.0.7:80
```

Explanation

ClusterIP

Internal IP of the Service.

Selector

Finds Pods having label

```
app=nginx
```

Endpoints

Actual Pod IP addresses behind the Service.

---

## Step 6 - Test ClusterIP

Launch temporary Pod

```bash
kubectl run busybox --image=busybox --rm -it -- sh
```

Inside the Pod

```
wget -qO- http://nginx-clusterip
```

or

```
wget -qO- http://10.96.210.15
```

Exit

```
exit
```

---

## Step 7 - Test from Laptop

```
curl http://10.96.210.15
```

Result

```
Connection timed out
```

Reason

ClusterIP works only inside Kubernetes.

---

# NodePort Service

## What is NodePort?

NodePort exposes an application on a fixed port of every Kubernetes Node.

Example

```
Node IP

192.168.1.20

NodePort

30080
```

Application

```
http://192.168.1.20:30080
```

NodePort internally forwards requests to Pods.

---

## Architecture

```
Browser

     |

     ▼

Node IP : 30080

     |

     ▼

NodePort Service

     |

-------------------------

|          |            |

▼          ▼            ▼

Pod1      Pod2        Pod3
```

---

## Step 8 - Create NodePort Service

Create file

```bash
nano nodeport.yaml
```

Paste

```yaml
apiVersion: v1

kind: Service

metadata:
  name: nginx-nodeport

spec:

  type: NodePort

  selector:
    app: nginx

  ports:

  - port: 80

    targetPort: 80

    nodePort: 30080
```

Apply

```bash
kubectl apply -f nodeport.yaml
```

---

## Step 9 - Verify

```bash
kubectl get svc
```

Example

```
NAME              TYPE

nginx-clusterip   ClusterIP

nginx-nodeport    NodePort
```

---

## Step 10 - Describe NodePort

```bash
kubectl describe svc nginx-nodeport
```

Example

```
Type: NodePort

Port: 80/TCP

NodePort: 30080/TCP

Endpoints:

10.244.0.5:80

10.244.0.6:80

10.244.0.7:80
```

Explanation

Port

Service Port

TargetPort

Container Port

NodePort

Port opened on every Kubernetes Node.

---

## Step 11 - Access Application

For cloud or bare-metal clusters:

```
http://<Node-IP>:30080
```

Example

```
http://192.168.1.20:30080
```

### Using Kind

Kind runs Kubernetes nodes as Docker containers, so NodePorts are **not directly accessible** from your laptop unless you configure port mappings.

A simple way to test is:

```bash
kubectl port-forward service/nginx-nodeport 8080:80
```

Now open:

```
http://localhost:8080
```

---

# Understanding Service Ports

```
Client

 |

 ▼

Port 80

 |

 ▼

Service

 |

 ▼

TargetPort 80

 |

 ▼

Container
```

Example

```yaml
ports:

- port: 80

  targetPort: 80

  nodePort: 30080
```

Meaning

Port

Service Port

TargetPort

Container Port

NodePort

External Port on every Kubernetes Node.

---

# Labels and Selectors

Deployment

```yaml
labels:

  app: nginx
```

Service

```yaml
selector:

  app: nginx
```

If labels match

↓

Pods become Endpoints.

If labels do not match

↓

Service has zero Endpoints.

---

# Endpoints

View Endpoints

```bash
kubectl get endpoints
```

Describe

```bash
kubectl describe endpoints nginx-clusterip
```

Example

```
10.244.0.5

10.244.0.6

10.244.0.7
```

---

# DNS

Kubernetes automatically creates DNS.

Instead of Pod IP

```
10.96.210.15
```

Use

```
http://nginx-clusterip
```

Inside same namespace.

Fully Qualified DNS

```
http://nginx-clusterip.default.svc.cluster.local
```

---

# Useful Commands

## Deployment

```bash
kubectl get deploy

kubectl describe deploy nginx-deployment

kubectl delete deployment nginx-deployment
```

## Pods

```bash
kubectl get pods

kubectl get pods -o wide

kubectl describe pod <pod-name>

kubectl logs <pod-name>
```

## Services

```bash
kubectl get svc

kubectl get svc -o wide

kubectl describe svc nginx-clusterip

kubectl describe svc nginx-nodeport

kubectl delete svc nginx-clusterip

kubectl delete svc nginx-nodeport
```

## Endpoints

```bash
kubectl get endpoints

kubectl describe endpoints nginx-clusterip
```

## YAML

```bash
kubectl apply -f deployment.yaml

kubectl apply -f clusterip.yaml

kubectl apply -f nodeport.yaml
```

Delete

```bash
kubectl delete -f deployment.yaml

kubectl delete -f clusterip.yaml

kubectl delete -f nodeport.yaml
```

---

# ClusterIP vs NodePort

| Feature | ClusterIP | NodePort |
|----------|-----------|-----------|
| Default Service | Yes | No |
| Internal Access | Yes | Yes |
| External Access | No | Yes (or via port mapping in Kind) |
| Stable IP | Yes | Yes |
| Load Balancing | Yes | Yes |
| DNS | Yes | Yes |
| Uses Pod Labels | Yes | Yes |
| Exposed on Node | No | Yes |

---

# Summary

ClusterIP

- Default Kubernetes Service.
- Used for communication between applications inside the cluster.
- Accessible only from within the cluster.
- Provides a stable virtual IP and DNS name.
- Automatically load-balances traffic across matching Pods.

NodePort

- Builds on ClusterIP by exposing the Service on a port of every Kubernetes node.
- Allows external access using `NodeIP:NodePort`.
- In Kind, NodePorts require port mappings or `kubectl port-forward` for access from your host machine.
- Still provides internal access through its ClusterIP.