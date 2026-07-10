# Kubernetes Node Affinity

## What is Node Affinity?

**Node Affinity** is a scheduling feature that tells Kubernetes **which nodes a Pod should (or prefers to) run on based on node labels**.

It is the **advanced version of `nodeSelector`**.

Think of it like this:

```text
nodeSelector
      │
      ▼
Simple Scheduling

Node Affinity
      │
      ▼
Advanced Scheduling
```

---

# Why was Node Affinity introduced?

`nodeSelector` can only perform an **exact match**.

Example:

```yaml
nodeSelector:
  hardware: gpu
```

Meaning:

```text
hardware == gpu
```

This is very limited.

Node Affinity allows us to create much more flexible scheduling rules like:

- Run on GPU nodes.
- Run on GPU OR SSD nodes.
- Avoid DEV nodes.
- Prefer Production nodes.
- Check multiple labels together.

---

# nodeSelector vs Node Affinity

## nodeSelector

```yaml
spec:
  nodeSelector:
    hardware: gpu
```

Only supports exact matching.

---

## Node Affinity

```yaml
spec:
  affinity:
    nodeAffinity:
      ...
```

Supports multiple operators:

- In
- NotIn
- Exists
- DoesNotExist
- Gt
- Lt

It is much more powerful than `nodeSelector`.

---

# Types of Node Affinity

There are two types.

## 1. requiredDuringSchedulingIgnoredDuringExecution

This is a **Hard Rule**.

Meaning:

- The Pod **must** satisfy the affinity rule.
- If no matching node exists, the Pod remains **Pending**.

---

## 2. preferredDuringSchedulingIgnoredDuringExecution

This is a **Soft Rule**.

Meaning:

- Kubernetes tries to schedule the Pod on matching nodes.
- If none are available, it schedules the Pod elsewhere.

---

# Understanding the Long Name

## requiredDuringSchedulingIgnoredDuringExecution

Break it into three parts.

### required

The rule **must** be satisfied.

If not:

```text
Pod
 ↓
Pending
```

---

### DuringScheduling

The rule is checked **only when the scheduler is selecting a node**.

---

### IgnoredDuringExecution

Once the Pod is running:

- Kubernetes does **not** recheck the rule.
- If node labels change later, the Pod continues running.

---

# Example Cluster

```
kind-worker
------------------
No hardware label

kind-worker2
------------------
hardware=gpu
```

---

# Example YAML

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: affinity-pod

spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: hardware
            operator: In
            values:
            - gpu

  containers:
  - name: nginx
    image: nginx
```

Apply:

```bash
kubectl apply -f affinity-pod.yaml
```

---

# Understanding the YAML

## affinity

```yaml
affinity:
```

Used to define scheduling rules.

It contains:

```text
affinity
│
├── nodeAffinity
├── podAffinity
└── podAntiAffinity
```

---

## nodeAffinity

```yaml
nodeAffinity:
```

Scheduling is based on **Node Labels**.

---

## requiredDuringSchedulingIgnoredDuringExecution

```yaml
requiredDuringSchedulingIgnoredDuringExecution:
```

Means:

- Required while scheduling.
- Ignore label changes after scheduling.

---

## nodeSelectorTerms

```yaml
nodeSelectorTerms:
```

Represents a list of scheduling conditions.

---

## matchExpressions

```yaml
matchExpressions:
```

Each expression represents one rule.

---

## key

```yaml
key: hardware
```

Scheduler checks whether the node has a label called:

```text
hardware
```

---

## operator

```yaml
operator: In
```

Means:

```text
Label value must be one of the specified values.
```

---

## values

```yaml
values:
- gpu
```

The complete condition becomes:

```text
hardware IN (gpu)
```

Similar to SQL:

```sql
hardware IN ('gpu')
```

---

# Scheduler Decision Process

Scheduler checks every node.

```
Need:

hardware IN (gpu)

↓

Checking Nodes

kind-control-plane
------------------

hardware=gpu ?

No

Reject

----------------------------

kind-worker

hardware=gpu ?

No

Reject

----------------------------

kind-worker2

hardware=gpu

Matches

↓

Schedule Pod
```

---

# Practical Experiment

Create the Pod.

```bash
kubectl apply -f affinity-pod.yaml
```

Check:

```bash
kubectl get pods -o wide
```

Expected:

```text
affinity-pod

Running

NODE:
kind-worker2
```

---

# IgnoredDuringExecution Demo

After the Pod starts running:

Remove the label.

```bash
kubectl label node kind-worker2 hardware-
```

Existing Pod:

```text
Running
```

Reason:

```
IgnoredDuringExecution
```

Now create another Pod using the same YAML.

Expected:

```text
Pending
```

Reason:

No node satisfies:

```text
hardware=gpu
```

---

# Scheduler Always Checks Multiple Conditions

A Pod is scheduled **only if every required condition is satisfied**.

Example:

```
Node Affinity
      ✔

Node Selector
      ✔

Resources Available
      ✔

Taints/Tolerations
      ❌

────────────────────────

Result

Cannot Schedule
```

Another example:

```
Node Affinity
      ✔

Node Selector
      ✔

Resources Available
      ✔

Taints/Tolerations
      ✔

────────────────────────

Result

Pod Scheduled
```

Another example:

```
Node Affinity
      ❌

Node Selector
      ✔

Resources Available
      ✔

Taints/Tolerations
      ✔

────────────────────────

Result

Cannot Schedule
```

Another example:

```
Node Affinity
      ✔

Node Selector
      ✔

Resources Available
      ❌

Taints/Tolerations
      ✔

────────────────────────

Result

Cannot Schedule
```

---

# Why My Pod Was Pending?

My cluster:

```
kind-control-plane
Taint:
node-role.kubernetes.io/control-plane:NoSchedule

kind-worker
No hardware label

kind-worker2
Label:
hardware=gpu

Taint:
hardware=gpu:NoSchedule
```

The Pod had:

- Node Affinity ✔
- No Toleration ❌

Scheduler checked:

```
kind-worker

Affinity

❌ Failed

----------------------

kind-control-plane

Affinity

❌ Failed

Taint

❌ Failed

----------------------

kind-worker2

Affinity

✔ Passed

Taint

❌ Failed

----------------------

Final Result

Pending
```

Error:

```text
0/3 nodes are available:
1 node(s) didn't match Pod's node affinity/selector,
2 node(s) had untolerated taint(s).
```

Reason:

Even though the node matched the affinity rule,

the Pod **did not tolerate the taint**.

---

# Useful Commands

## View Node Labels

```bash
kubectl get nodes --show-labels
```

---

## Add Label

```bash
kubectl label node kind-worker2 hardware=gpu
```

---

## Remove Label

```bash
kubectl label node kind-worker2 hardware-
```

---

## View Node Details

```bash
kubectl describe node kind-worker2
```

---

## View Pod Details

```bash
kubectl describe pod affinity-pod
```

---

# Node Affinity vs nodeSelector

| nodeSelector | Node Affinity |
|--------------|---------------|
| Simple | Advanced |
| Exact Match | Multiple Operators |
| One Condition | Multiple Conditions |
| Easy | Flexible |

---

# Memory Trick

```
Labels
      │
      ▼
Describe Nodes

↓

nodeSelector
      │
      ▼
Simple Exact Match

↓

Node Affinity
      │
      ▼
Advanced Scheduling Rules

↓

Scheduler Checks Everything

Node Affinity
Node Selector
Resources
Taints/Tolerations

↓

All Passed ?

YES  → Schedule Pod

NO   → Pending
```