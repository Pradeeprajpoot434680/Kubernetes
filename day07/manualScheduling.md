# Kubernetes Node Labels & Node Selectors

## Objective

In this lab, you will learn:

- What Node Labels are
- Built-in Node Labels
- Custom Node Labels
- Why Node Labels are required
- What `nodeSelector` is
- How Kubernetes Scheduler uses `nodeSelector`
- Scheduling Pods on specific nodes
- Practical demonstrations
- Production use cases
- Best practices
- Interview questions

---

# Why Do We Need Node Labels?

In a real Kubernetes cluster, every worker node is not identical.

Example:

```text
                    Kubernetes Cluster

          ┌───────────────┐
          │    Node-1     │
          │ 16 CPU        │
          │ SSD           │
          └───────────────┘

          ┌───────────────┐
          │    Node-2     │
          │ 4 CPU         │
          │ HDD           │
          └───────────────┘

          ┌───────────────┐
          │    Node-3     │
          │ NVIDIA GPU    │
          │ SSD           │
          └───────────────┘
```

Suppose your Machine Learning application requires a GPU.

Should Kubernetes schedule it on Node-2?

No.

The Scheduler must know which nodes have GPUs.

This is solved using **Node Labels**.

---

# What is a Node Label?

A **Node Label** is a key-value pair attached to a Kubernetes Node.

Example:

```text
Node-1

disk=ssd
cpu=16
region=india
```

Another node:

```text
Node-2

disk=hdd
cpu=4
```

Another node:

```text
Node-3

gpu=true
disk=ssd
```

The Scheduler reads these labels before deciding where to run Pods.

---

# Node Labels Architecture

```text
                   Scheduler

                        │

        ┌───────────────┼───────────────┐
        ▼               ▼               ▼

     Node-1         Node-2          Node-3

   disk=ssd       disk=hdd        gpu=true
   cpu=16         cpu=4           cpu=32
```

---

# View Nodes

```bash
kubectl get nodes
```

Example:

```text
NAME
worker
worker2
worker3
```

---

# View All Node Labels

```bash
kubectl get nodes --show-labels
```

Example:

```text
worker

kubernetes.io/os=linux

kubernetes.io/hostname=worker

kubernetes.io/arch=amd64
```

---

# Describe a Node

```bash
kubectl describe node worker
```

Look for:

```text
Labels:
```

Example:

```text
kubernetes.io/os=linux
kubernetes.io/arch=amd64
kubernetes.io/hostname=worker
```

---

# Built-in Node Labels

Kubernetes automatically assigns many labels.

| Label | Meaning |
|--------|----------|
| kubernetes.io/os | Operating System |
| kubernetes.io/arch | CPU Architecture |
| kubernetes.io/hostname | Node Name |
| topology.kubernetes.io/region | Cloud Region |
| topology.kubernetes.io/zone | Availability Zone |
| node.kubernetes.io/instance-type | Machine Type |

Example:

```text
kubernetes.io/os=linux
```

means:

```
Linux Node
```

---

# Custom Node Labels

You can assign your own labels.

Examples:

```text
disk=ssd
```

```text
gpu=true
```

```text
environment=production
```

```text
team=backend
```

---

# Add a Node Label

Label a node with SSD storage.

```bash
kubectl label node worker disk=ssd
```

Verify:

```bash
kubectl describe node worker
```

---

Add a GPU label.

```bash
kubectl label node worker2 gpu=true
```

---

# Update a Label

```bash
kubectl label node worker gpu=false --overwrite
```

---

# Remove a Label

```bash
kubectl label node worker gpu-
```

---

# Why Node Labels Alone Are Not Enough

Labels only describe nodes.

They do not influence scheduling.

To tell Kubernetes **where** a Pod should run, we use **nodeSelector**.

---

# What is nodeSelector?

`nodeSelector` is a scheduling constraint.

It tells the Kubernetes Scheduler:

> Run this Pod only on Nodes that have the specified label.

---

# Example

Suppose:

```text
Node-1

disk=ssd
```

```text
Node-2

disk=hdd
```

The application requires SSD storage.

Pod YAML:

```yaml
spec:

  nodeSelector:
    disk: ssd
```

Scheduler workflow:

```text
Read nodeSelector

↓

disk=ssd

↓

Check every node

↓

Find matching node

↓

Schedule Pod
```

---

# Pod Example

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: nginx

spec:

  nodeSelector:
    disk: ssd

  containers:
  - name: nginx
    image: nginx
```

---

# Scheduler Flow

```text
Scheduler

↓

Read nodeSelector

↓

disk=ssd

↓

Check all nodes

↓

Find matching node

↓

Schedule Pod
```

---

# What Happens If No Node Matches?

Suppose the Pod requires:

```yaml
nodeSelector:
  disk: nvme
```

But the cluster has only:

```text
disk=ssd
disk=hdd
```

Result:

```bash
kubectl get pods
```

Output:

```text
NAME       READY   STATUS
nginx      0/1     Pending
```

Describe the Pod:

```bash
kubectl describe pod nginx
```

Events:

```text
0/3 nodes are available

No nodes match node selector
```

---

# Practical Demonstration

## Step 1 – Create a Multi-Node Kind Cluster

Create a file:

```text
kind-config.yaml
```

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4

nodes:
- role: control-plane
- role: worker
- role: worker
```

Create the cluster:

```bash
kind create cluster --name scheduler-demo --config kind-config.yaml
```

---

## Step 2 – Verify Nodes

```bash
kubectl get nodes
```

Example:

```text
scheduler-demo-control-plane
scheduler-demo-worker
scheduler-demo-worker2
```

---

## Step 3 – Add Custom Labels

Worker-1:

```bash
kubectl label node scheduler-demo-worker disk=ssd
```

Worker-2:

```bash
kubectl label node scheduler-demo-worker2 disk=hdd
```

GPU Node:

```bash
kubectl label node scheduler-demo-worker gpu=true
```

Verify:

```bash
kubectl get nodes --show-labels
```

---

## Step 4 – Deploy a Pod Using nodeSelector

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: nginx-ssd

spec:

  nodeSelector:
    disk: ssd

  containers:
  - name: nginx
    image: nginx
```

Deploy:

```bash
kubectl apply -f nginx-ssd.yaml
```

Check:

```bash
kubectl get pods -o wide
```

Observe the **NODE** column.

The Pod should be running on the node labeled `disk=ssd`.

---

## Step 5 – Demonstrate a Scheduling Failure

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: gpu-pod

spec:

  nodeSelector:
    gpu: "false"

  containers:
  - name: nginx
    image: nginx
```

Deploy:

```bash
kubectl apply -f gpu-pod.yaml
```

Verify:

```bash
kubectl get pods
```

Expected:

```text
gpu-pod   0/1   Pending
```

Describe:

```bash
kubectl describe pod gpu-pod
```

Look for scheduling events indicating that no nodes matched the selector.

---

# Real Production Examples

## GPU Nodes

```text
gpu=true
```

```yaml
nodeSelector:
  gpu: "true"
```

---

## SSD Storage

```text
disk=ssd
```

```yaml
nodeSelector:
  disk: ssd
```

---

## Linux Nodes

```yaml
nodeSelector:
  kubernetes.io/os: linux
```

---

## ARM Architecture

```yaml
nodeSelector:
  kubernetes.io/arch: arm64
```

---

## Production Environment

```yaml
nodeSelector:
  environment: production
```

---

# Limitations of nodeSelector

`nodeSelector` supports only exact key-value matching.

Examples it **cannot** express:

- SSD **or** NVMe
- Prefer SSD but allow HDD
- Avoid GPU nodes
- Complex scheduling rules

For these scenarios, Kubernetes provides **Node Affinity**.

---

# Best Practices

- Use meaningful labels such as:
  - `disk=ssd`
  - `gpu=true`
  - `environment=production`
- Prefer built-in labels whenever possible.
- Keep label names consistent across the cluster.
- Do not hard-code node names in application manifests.

---

# Summary

| Feature | Description |
|----------|-------------|
| Node Label | Metadata attached to a Node |
| Built-in Labels | Automatically created by Kubernetes |
| Custom Labels | User-defined node metadata |
| nodeSelector | Schedules Pods to nodes with matching labels |
| No Matching Node | Pod remains in Pending state |

---

# Interview Questions

### What is a Node Label?

A key-value pair attached to a Kubernetes Node that describes its characteristics.

---

### What is nodeSelector?

A scheduling constraint that tells Kubernetes to schedule a Pod only on nodes with matching labels.

---

### What happens if no node satisfies the nodeSelector?

The Pod remains in the **Pending** state until a matching node becomes available or the selector is changed.

---

### Can a node have multiple labels?

Yes.

Example:

```text
disk=ssd
gpu=true
environment=production
region=india
```

---

### What is the difference between Pod Labels and Node Labels?

| Pod Labels | Node Labels |
|-------------|-------------|
| Identify Pods | Identify Nodes |
| Used by Services and Deployments | Used by the Scheduler |
| Help select application Pods | Help decide where Pods run |

---

# Next Topic

The next topic is **Manual Scheduling**, where we'll learn:

- How the Kubernetes Scheduler normally works
- What happens when there is no Scheduler
- How to manually assign a Pod to a Node using `spec.nodeName`
- Differences between `nodeSelector`, `nodeName`, and Scheduler
- Practical demonstrations with multi-node clusters
- Real-world use cases and interview questions