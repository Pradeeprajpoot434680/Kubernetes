# Kubernetes Taints and Tolerations

## What are Taints and Tolerations?

Taints and tolerations are Kubernetes features that **control where Pods can run**.

- **Taint** → Applied on a **Node**.
- **Toleration** → Applied on a **Pod**.

Think of them as:

- **Node:** "Don't schedule Pods here unless they are allowed."
- **Pod:** "I am allowed to run on this tainted node."

---

# Why do we need Taints and Tolerations?

Suppose your cluster has two nodes.

```text
kind-worker
    │
    ├── Normal Applications

kind-worker2
    │
    ├── GPU Machine
```

Without taints:

- Any Pod can be scheduled on any node.
- Even a simple nginx Pod may run on the expensive GPU node.

This wastes resources.

To protect the GPU node, we use:

- Label
- nodeSelector
- Taint
- Toleration

---

# Labels

A label is simply metadata attached to a node.

Example:

```bash
kubectl label node kind-worker2 hardware=gpu
```

Verify:

```bash
kubectl get nodes --show-labels
```

Output:

```text
kind-worker2

hardware=gpu
```

A label only describes a node.

It **does not restrict scheduling**.

---

# nodeSelector

A Pod uses **nodeSelector** to request a particular node.

Example:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: gpu-pod

spec:
  nodeSelector:
    hardware: gpu

  containers:
  - name: nginx
    image: nginx
```

The scheduler looks for a node having:

```text
hardware=gpu
```

If found, the Pod is scheduled there.

---

# Taints

A taint is applied to a node.

Syntax:

```bash
kubectl taint node <node-name> key=value:<effect>
```

Example:

```bash
kubectl taint node kind-worker2 hardware=gpu:NoSchedule
```

Verify:

```bash
kubectl describe node kind-worker2
```

Output:

```text
Taints:
hardware=gpu:NoSchedule
```

Now the node is saying:

> "Don't schedule Pods here unless they tolerate this taint."

---

# Tolerations

A Pod uses tolerations to tell Kubernetes:

> "I can run on this tainted node."

Example:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ml-app

spec:

  nodeSelector:
    hardware: gpu

  tolerations:
  - key: "hardware"
    operator: Equal
    value: "gpu"
    effect: "NoSchedule"

  containers:
  - name: app
    image: nginx
```

Now the scheduler sees:

- Node label matches.
- Pod tolerates the taint.

Result:

```text
Pod → Scheduled Successfully
```

---

# Labels vs nodeSelector vs Taints vs Tolerations

| Feature | Applied On | Purpose |
|----------|------------|----------|
| Label | Node | Describe a node |
| nodeSelector | Pod | Select nodes using labels |
| Taint | Node | Prevent Pods from scheduling |
| Toleration | Pod | Allow Pod to run on tainted node |

---

# Practical Example

## Current Cluster

```text
kind-worker
--------------
No Label
No Taint

kind-worker2
--------------
Label:
hardware=gpu

Taint:
hardware=gpu:NoSchedule
```

---

## Example 1 : Pod without toleration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: gpu-pod

spec:

  nodeSelector:
    hardware: gpu

  containers:
  - name: nginx
    image: nginx
```

Apply:

```bash
kubectl apply -f gpu-pod.yaml
```

Check:

```bash
kubectl get pods
```

Expected:

```text
STATUS: Pending
```

Reason:

```text
Pod selected the GPU node,
but the node rejected it because
the Pod has no toleration.
```

Describe the Pod:

```bash
kubectl describe pod gpu-pod
```

Typical Event:

```text
0/2 nodes are available:
1 node(s) had untolerated taint {hardware: gpu}
```

---

## Example 2 : Pod with toleration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ml-app

spec:

  nodeSelector:
    hardware: gpu

  tolerations:
  - key: hardware
    operator: Equal
    value: gpu
    effect: NoSchedule

  containers:
  - name: nginx
    image: nginx
```

Apply:

```bash
kubectl apply -f ml-app.yaml
```

Check:

```bash
kubectl get pods -o wide
```

Output:

```text
NAME      NODE

ml-app    kind-worker2
```

Reason:

- nodeSelector found the GPU node.
- toleration allowed scheduling.

---

# Taint Effects

Kubernetes supports three taint effects.

## 1. NoSchedule

Meaning:

> Do not schedule new Pods unless they tolerate the taint.

Existing Pods continue running.

### Apply

```bash
kubectl taint node kind-worker2 hardware=gpu:NoSchedule
```

### Remove

```bash
kubectl taint node kind-worker2 hardware=gpu:NoSchedule-
```

---

### Example

Node:

```text
kind-worker2

Label:
hardware=gpu

Taint:
hardware=gpu:NoSchedule
```

Pod:

```yaml
nodeSelector:
  hardware: gpu

# No toleration
```

Result:

```text
Pending
```

Pod with toleration:

```yaml
tolerations:
- key: hardware
  operator: Equal
  value: gpu
  effect: NoSchedule
```

Result:

```text
Running
```

---

## 2. PreferNoSchedule

Meaning:

> Avoid scheduling Pods here if possible.

It is only a preference.

### Apply

```bash
kubectl taint node kind-worker2 hardware=gpu:PreferNoSchedule
```

### Remove

```bash
kubectl taint node kind-worker2 hardware=gpu:PreferNoSchedule-
```

Example:

```
worker-1
Available

worker-2
PreferNoSchedule
```

Scheduler chooses:

```text
worker-1
```

If worker-1 has no capacity:

```text
worker-2
```

---

## 3. NoExecute

Meaning:

- Do not schedule new Pods.
- Evict existing Pods without matching tolerations.

### Apply

```bash
kubectl taint node kind-worker2 hardware=gpu:NoExecute
```

### Remove

```bash
kubectl taint node kind-worker2 hardware=gpu:NoExecute-
```

---

### Example

Current Pods:

```text
kind-worker2

gpu-pod
ml-app
```

Apply:

```bash
kubectl taint node kind-worker2 hardware=gpu:NoExecute
```

Pod without matching toleration:

```text
Evicted
```

Pod with matching toleration:

```yaml
tolerations:
- key: hardware
  operator: Equal
  value: gpu
  effect: NoExecute
```

Result:

```text
Continues Running
```

---

# tolerationSeconds

This field works only with **NoExecute**.

Example:

```yaml
tolerations:
- key: hardware
  operator: Equal
  value: gpu
  effect: NoExecute
  tolerationSeconds: 30
```

Behavior:

```text
Apply NoExecute

↓

Pod continues running

↓

30 seconds later

↓

Evicted
```

---

# Useful Commands

## Add Label

```bash
kubectl label node kind-worker2 hardware=gpu
```

---

## View Labels

```bash
kubectl get nodes --show-labels
```

---

## Add Taint

```bash
kubectl taint node kind-worker2 hardware=gpu:NoSchedule
```

---

## Remove Taint

```bash
kubectl taint node kind-worker2 hardware=gpu:NoSchedule-
```

---

## View Taints

```bash
kubectl describe node kind-worker2
```

---

## View Pod Details

```bash
kubectl describe pod <pod-name>
```

---

## Check Scheduling Node

```bash
kubectl get pods -o wide
```

---

## Delete Pod

```bash
kubectl delete pod <pod-name>
```

---

## Apply YAML

```bash
kubectl apply -f pod.yaml
```

---

# Interview Tips

### Labels

> Describe a node.

---

### nodeSelector

> Select a node using labels.

---

### Taint

> Prevent Pods from running on a node.

---

### Toleration

> Allow a Pod to run on a tainted node.

---

# Memory Trick

```
Label
↓
"This node is a GPU node."

nodeSelector
↓
"I want a GPU node."

Taint
↓
"Don't enter."

Toleration
↓
"I have permission."

NoSchedule
↓
Stop new Pods.

PreferNoSchedule
↓
Avoid new Pods if possible.

NoExecute
↓
Stop new Pods + Evict existing Pods.
```