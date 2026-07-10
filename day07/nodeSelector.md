# Kubernetes Node Labels and nodeSelector - Practical Guide

# 1. Introduction

Kubernetes uses **Node Labels** and **nodeSelector** to control where Pods run.

Basic idea:

```
Node Labels
     |
     |
     v

Identify nodes

     |
     |
     v

nodeSelector

     |
     |
     v

Choose matching nodes for Pods
```

---

# 2. What are Node Labels?

A label is a key-value pair attached to Kubernetes objects like Nodes.

Example:

```
environment=production

disk=ssd

role=backend
```

Labels describe the characteristics of a node.

Example:

```
Node: worker-1

Labels:

environment=production
disk=ssd
role=backend
```

---

# 3. View Node Labels

Command:

```bash
kubectl get nodes --show-labels
```

Example:

```
NAME                 LABELS

kind-worker
kubernetes.io/os=linux

kind-worker2
kubernetes.io/os=linux
```

---

# 4. Add Labels to Nodes

Syntax:

```bash
kubectl label node <node-name> key=value
```

Example:

```bash
kubectl label node kind-worker environment=production
```

Output:

```
node/kind-worker labeled
```

Check:

```bash
kubectl get nodes --show-labels
```

Output:

```
kind-worker

environment=production
```

---

# 5. Remove Node Labels

Syntax:

```bash
kubectl label node <node-name> key-
```

Example:

```bash
kubectl label node kind-worker environment-
```

The label is removed.

---

# 6. What is nodeSelector?

`nodeSelector` is a Pod specification field that tells Kubernetes:

> Run this Pod only on nodes having this label.

Example:

```
Node:

environment=production


Pod:

nodeSelector:
  environment: production
```

Result:

```
Pod runs on production node
```

---

# 7. Basic nodeSelector Example

## Step 1: Label a Node

Example:

```bash
kubectl label node kind-worker environment=production
```

Verify:

```bash
kubectl get nodes --show-labels
```

---

## Step 2: Create Pod YAML

File:

```
production-pod.yaml
```

```yaml
apiVersion: v1

kind: Pod

metadata:
  name: production-nginx


spec:

  nodeSelector:

    environment: production


  containers:

  - name: nginx

    image: nginx
```

---

## Step 3: Create Pod

```bash
kubectl apply -f production-pod.yaml
```

---

## Step 4: Check Pod Location

```bash
kubectl get pods -o wide
```

Example:

```
NAME              NODE

production-nginx  kind-worker
```

The Pod runs only on nodes having:

```
environment=production
```

---

# Practical Use Case 1: Production and Testing Separation

## Scenario

Cluster:

```
worker-1

environment=production


worker-2

environment=testing
```

Production application:

```yaml
spec:

  nodeSelector:

    environment: production
```

Result:

```
Application
     |
     |
worker-1
```

It will not run on testing nodes.

---

# Practical Use Case 2: GPU Workloads

Machine learning workloads need GPU nodes.

## Label GPU Node

```bash
kubectl label node gpu-node accelerator=nvidia
```

Node:

```
gpu-node

accelerator=nvidia
```

---

## Pod YAML

```yaml
apiVersion: v1

kind: Pod

metadata:
  name: ml-workload


spec:

  nodeSelector:

    accelerator: nvidia


  containers:

  - name: tensorflow

    image: tensorflow/tensorflow
```

Result:

```
ml-workload

      |

      v

gpu-node
```

The workload only runs on GPU machines.

---

# Practical Use Case 3: Database on SSD Nodes

## Label SSD Nodes

```bash
kubectl label node worker1 storage=ssd
```

Node:

```
worker1

storage=ssd
```

---

## Database Pod

```yaml
apiVersion: v1

kind: Pod

metadata:
  name: database


spec:

  nodeSelector:

    storage: ssd


  containers:

  - name: mysql

    image: mysql
```

Result:

```
Database Pod

      |

      v

SSD Node
```

---

# Practical Use Case 4: Frontend and Backend Separation

## Label Nodes

Frontend:

```bash
kubectl label node worker1 role=frontend
```

Backend:

```bash
kubectl label node worker2 role=backend
```

---

## Frontend Pod

```yaml
spec:

  nodeSelector:

    role: frontend
```

Runs:

```
worker1
```

---

## Backend Pod

```yaml
spec:

  nodeSelector:

    role: backend
```

Runs:

```
worker2
```

---

# 8. Debug nodeSelector Problems

If a Pod is stuck:

Check:

```bash
kubectl get pods
```

Example:

```
Pending
```

Describe:

```bash
kubectl describe pod <pod-name>
```

Possible message:

```
0/3 nodes are available:
node(s) didn't match Pod's node selector
```

Meaning:

The Pod requested a label that no node has.

Example:

Pod wants:

```yaml
nodeSelector:

  gpu: true
```

But nodes have:

```
gpu=false
```

No scheduling happens.

---

# 9. Node Labels vs nodeSelector

| Node Labels | nodeSelector |
|---|---|
| Attached to Nodes | Attached to Pods |
| Describe node properties | Select nodes |
| Example: disk=ssd | Example: disk=ssd |
| Created using kubectl label | Written in Pod YAML |

---

# 10. Common Real-World Examples

## Logging Agents

Node label:

```
type=worker
```

DaemonSet:

```yaml
nodeSelector:

  type: worker
```

Runs logging agents only on worker nodes.

---

## Database Servers

Node label:

```
storage=ssd
```

Database Pod:

```yaml
nodeSelector:

  storage=ssd
```

Runs database on SSD machines.

---

## High Memory Applications

Node label:

```
memory=high
```

Application:

```yaml
nodeSelector:

  memory=high
```

Runs only on high-memory nodes.

---

# 11. Important Commands

## List Nodes

```bash
kubectl get nodes
```

---

## Show Labels

```bash
kubectl get nodes --show-labels
```

---

## Add Label

```bash
kubectl label node worker1 role=backend
```

---

## Remove Label

```bash
kubectl label node worker1 role-
```

---

## Describe Node

```bash
kubectl describe node worker1
```

---

## Check Pod Node

```bash
kubectl get pods -o wide
```

---

# 12. Complete Workflow

```
1. Create nodes

        |

2. Add labels

        |

3. Create Pod with nodeSelector

        |

4. Scheduler checks labels

        |

5. Pod runs on matching node
```

---

# Key Points

- Labels are key-value pairs attached to nodes.
- nodeSelector uses labels to choose nodes.
- If no matching node exists, the Pod remains Pending.
- nodeSelector is simple and strict.
- Used for:
  - GPU workloads
  - Database workloads
  - Production isolation
  - Storage-based scheduling
  - Environment separation

---

# Final Rule

```
Labels describe nodes.

nodeSelector chooses nodes.

No matching label = No Pod scheduling.
```