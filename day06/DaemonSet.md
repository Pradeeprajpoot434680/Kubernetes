# Kubernetes DaemonSet - Practical Guide

## What is a DaemonSet?

A **DaemonSet** ensures that **one Pod runs on every matching node** in the cluster.

For example:

```
Cluster

Control Plane
Worker-1
Worker-2
```

If a DaemonSet matches all worker nodes:

```
Worker-1  ---> Busybox Pod
Worker-2  ---> Busybox Pod
```

If a new worker node is added:

```
Worker-3  ---> Busybox Pod (Created Automatically)
```

---

# Step 1: Check Your Nodes

```bash
kubectl get nodes
```

Example:

```text
NAME                 STATUS   ROLES
kind-control-plane   Ready    control-plane
kind-worker          Ready    <none>
kind-worker2         Ready    <none>
```

---

# Step 2: View Node Labels

Every node has labels.

```bash
kubectl get nodes --show-labels
```

Example:

```text
kind-worker
kubernetes.io/os=linux
kubernetes.io/hostname=kind-worker
```

Notice there is **no custom label** like:

```
role=worker
```

---

# Step 3: Add Labels to Worker Nodes

Label Worker-1

```bash
kubectl label node kind-worker role=worker
```

Label Worker-2

```bash
kubectl label node kind-worker2 role=worker
```

Output:

```text
node/kind-worker labeled
node/kind-worker2 labeled
```

Verify:

```bash
kubectl get nodes --show-labels
```

Output:

```text
kind-worker
...
role=worker

kind-worker2
...
role=worker
```

---

# Step 4: Create a DaemonSet

Create a file named **daemonset.yaml**

```yaml
apiVersion: apps/v1
kind: DaemonSet

metadata:
  name: busybox-daemon

spec:
  selector:
    matchLabels:
      app: busybox

  template:
    metadata:
      labels:
        app: busybox

    spec:
      nodeSelector:
        role: worker

      containers:
      - name: busybox
        image: busybox
        command:
        - sh
        - -c
        - |
          while true; do
            echo "Running on $(hostname)"
            sleep 10
          done
```

---

# Step 5: Create the DaemonSet

```bash
kubectl apply -f daemonset.yaml
```

Output:

```text
daemonset.apps/busybox-daemon created
```

---

# Step 6: Check the DaemonSet

```bash
kubectl get ds
```

Example:

```text
NAME              DESIRED   CURRENT   READY
busybox-daemon    2         2         2
```

Explanation:

```
DESIRED = 2
```

Because only two nodes have:

```
role=worker
```

---

# Step 7: Verify Pods

```bash
kubectl get pods -o wide
```

Example:

```text
NAME                     READY   STATUS    NODE
busybox-daemon-abcde     1/1     Running   kind-worker
busybox-daemon-fghij     1/1     Running   kind-worker2
```

Notice:

One Pod is running on each worker node.

---

# Step 8: View Pod Logs

```bash
kubectl logs <pod-name>
```

Example:

```bash
kubectl logs busybox-daemon-abcde
```

Output:

```text
Running on busybox-daemon-abcde
Running on busybox-daemon-abcde
Running on busybox-daemon-abcde
```

---

# Step 9: Delete One Pod

Delete a Pod.

```bash
kubectl delete pod <pod-name>
```

Watch Pods:

```bash
kubectl get pods -w
```

Example:

```text
busybox-daemon-abcde   Terminating
busybox-daemon-xyz12   ContainerCreating
busybox-daemon-xyz12   Running
```

DaemonSet automatically recreates the Pod.

---

# Step 10: Remove a Node Label

Remove the label.

```bash
kubectl label node kind-worker role-
```

Verify:

```bash
kubectl get nodes --show-labels
```

Now check:

```bash
kubectl get ds
```

Example:

```text
NAME              DESIRED   CURRENT
busybox-daemon    1         1
```

The Pod on that node is removed because it no longer matches the selector.

---

# Step 11: Add the Label Again

```bash
kubectl label node kind-worker role=worker
```

Check:

```bash
kubectl get ds
```

Example:

```text
NAME              DESIRED   CURRENT
busybox-daemon    2         2
```

A new Pod is automatically created.

---

# Step 12: Describe the DaemonSet

```bash
kubectl describe ds busybox-daemon
```

Useful information:

- Desired Pods
- Current Pods
- Ready Pods
- Events
- Node Selector

---

# Step 13: Describe a Node

```bash
kubectl describe node kind-worker
```

Useful information:

- Labels
- Taints
- Capacity
- Allocatable Resources
- Running Pods

---

# Step 14: Delete the DaemonSet

```bash
kubectl delete ds busybox-daemon
```

Check Pods:

```bash
kubectl get pods
```

Output:

```text
No resources found
```

All DaemonSet Pods are deleted.

---

# Important Commands

## Show Nodes

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
kubectl label node kind-worker role=worker
```

---

## Remove Label

```bash
kubectl label node kind-worker role-
```

---

## Create DaemonSet

```bash
kubectl apply -f daemonset.yaml
```

---

## View DaemonSets

```bash
kubectl get ds
```

---

## View Pods

```bash
kubectl get pods -o wide
```

---

## Watch Pods

```bash
kubectl get pods -w
```

---

## View Logs

```bash
kubectl logs <pod-name>
```

---

## Describe DaemonSet

```bash
kubectl describe ds busybox-daemon
```

---

## Describe Node

```bash
kubectl describe node kind-worker
```

---

## Delete Pod

```bash
kubectl delete pod <pod-name>
```

---

## Delete DaemonSet

```bash
kubectl delete ds busybox-daemon
```

---

# Key Points

- A DaemonSet creates **one Pod per matching node**.
- `nodeSelector` determines **which nodes are eligible**.
- Node labels determine whether a node matches the selector.
- If a node gains the required label, the DaemonSet creates a Pod there automatically.
- If a node loses the required label, the DaemonSet removes its Pod.
- Deleting a DaemonSet-managed Pod causes the DaemonSet to recreate it.
- Deleting the DaemonSet removes all Pods that it manages.