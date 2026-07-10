# Kubernetes Multi-Container Pod Lab

## Objective

In this lab, we will learn how Kubernetes Pods can contain multiple containers.

By the end of this lab, you will understand:

- Creating a single-container Pod
- Creating a multi-container Pod
- Viewing logs of individual containers
- Executing commands inside specific containers
- Shared networking between containers
- Shared storage using `emptyDir`
- Init Containers
- Multiple Init Containers
- Pod lifecycle
- Debugging multi-container Pods

---

# Prerequisites

Before starting, make sure the following software is installed.

| Software | Purpose |
|----------|----------|
| Docker | Container Runtime |
| Kind | Local Kubernetes Cluster |
| kubectl | Kubernetes CLI |

---

# Step 1 – Verify Docker

Check Docker installation.

```bash
docker --version
```

Example output

```text
Docker version 28.x.x
```

---

# Step 2 – Verify Kind

```bash
kind version
```

Example

```text
kind v0.29.0
```

---

# Step 3 – Verify kubectl

```bash
kubectl version --client
```

---

# Step 4 – Check Kubernetes Cluster

```bash
kubectl cluster-info
```

If no cluster exists, create one.

```bash
kind create cluster --name multi-container-demo
```

---

# Step 5 – Verify Nodes

```bash
kubectl get nodes
```

Example

```text
NAME                                      STATUS
multi-container-demo-control-plane        Ready
```

---

# Create Namespace

Create a namespace for this lab.

```bash
kubectl create namespace demo
```

Verify

```bash
kubectl get namespaces
```

Set it as the default namespace.

```bash
kubectl config set-context --current --namespace=demo
```

Verify

```bash
kubectl config view --minify
```

---

# Lab 1 – Single Container Pod

## Create YAML

Create a file named:

```text
single-pod.yaml
```

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: nginx-single

spec:
  containers:
  - name: nginx
    image: nginx:latest
```

---

## Create Pod

```bash
kubectl apply -f single-pod.yaml
```

---

## Verify Pod

```bash
kubectl get pods
```

Expected

```text
NAME             READY   STATUS
nginx-single     1/1     Running
```

---

## Describe Pod

```bash
kubectl describe pod nginx-single
```

Observe

- Pod IP
- Events
- Container Status
- Image Name

---

## View Logs

```bash
kubectl logs nginx-single
```

---

## Delete Pod

```bash
kubectl delete pod nginx-single
```

---

# Lab 2 – Multi-Container Pod

## Objective

Create a Pod containing two containers.

- Nginx
- BusyBox

---

## YAML

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: multi-container

spec:

  containers:

  - name: nginx
    image: nginx

  - name: busybox
    image: busybox
    command:
      - sh
      - -c
      - |
        while true
        do
          echo "BusyBox Running..."
          sleep 5
        done
```

---

## Create Pod

```bash
kubectl apply -f multi-pod.yaml
```

---

## Verify

```bash
kubectl get pods
```

---

## Describe Pod

```bash
kubectl describe pod multi-container
```

Notice

```
Containers

nginx

busybox
```

---

# Logs

Since multiple containers exist, Kubernetes requires the container name.

BusyBox

```bash
kubectl logs multi-container -c busybox
```

Nginx

```bash
kubectl logs multi-container -c nginx
```

---

# Execute Inside BusyBox

```bash
kubectl exec -it multi-container -c busybox -- sh
```

Inside

```bash
hostname
```

Check IP

```bash
hostname -i
```

Exit

```bash
exit
```

---

# Execute Inside Nginx

```bash
kubectl exec -it multi-container -c nginx -- sh
```

Check IP

```bash
hostname -i
```

Observation

Both containers return the **same IP address** because they share the Pod network namespace.

---

# Lab 3 – Shared Volume

## Objective

Demonstrate shared storage using `emptyDir`.

One container writes data.

Another reads the same data.

---

## YAML

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: shared-volume

spec:

  volumes:
  - name: shared-data
    emptyDir: {}

  containers:

  - name: writer

    image: busybox

    command:
    - sh
    - -c
    - |
      while true
      do
        date >> /shared/log.txt
        sleep 3
      done

    volumeMounts:
    - name: shared-data
      mountPath: /shared

  - name: reader

    image: busybox

    command:
    - sh
    - -c
    - |
      while true
      do
        cat /shared/log.txt
        sleep 5
      done

    volumeMounts:
    - name: shared-data
      mountPath: /shared
```

---

## Create

```bash
kubectl apply -f shared-volume.yaml
```

---

## Logs

Writer

```bash
kubectl logs shared-volume -c writer
```

Reader

```bash
kubectl logs shared-volume -c reader
```

Observation

The reader continuously displays the data written by the writer.

This proves both containers are sharing the same volume.

---

# Lab 4 – Init Container

## Objective

Learn how Init Containers execute before the application starts.

---

## YAML

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: init-demo

spec:

  volumes:
  - name: data
    emptyDir: {}

  initContainers:

  - name: init-message

    image: busybox

    command:
    - sh
    - -c
    - |
      echo "Initialization Completed" > /data/message.txt

    volumeMounts:
    - name: data
      mountPath: /data

  containers:

  - name: app

    image: busybox

    command:
    - sh
    - -c
    - |
      cat /data/message.txt
      sleep 3600

    volumeMounts:
    - name: data
      mountPath: /data
```

---

## Create

```bash
kubectl apply -f init-container.yaml
```

---

## Describe

```bash
kubectl describe pod init-demo
```

Observe

```
Init Containers

↓

Completed

↓

Main Container Running
```

---

## Logs

Init Container

```bash
kubectl logs init-demo -c init-message
```

Main Container

```bash
kubectl logs init-demo -c app
```

Expected

```
Initialization Completed
```

---

# Lab 5 – Multiple Init Containers

## YAML

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: multi-init

spec:

  initContainers:

  - name: init-one

    image: busybox

    command:
    - sh
    - -c
    - |
      echo Init1
      sleep 5

  - name: init-two

    image: busybox

    command:
    - sh
    - -c
    - |
      echo Init2
      sleep 5

  containers:

  - name: app

    image: busybox

    command:
    - sh
    - -c
    - |
      echo App Started
      sleep 3600
```

---

## Create

```bash
kubectl apply -f multi-init.yaml
```

---

## Observe Lifecycle

```bash
kubectl get pods -w
```

Example

```
Init:0/2

↓

Init:1/2

↓

PodInitializing

↓

Running
```

---

# Inspect Pod

```bash
kubectl get pod multi-init -o yaml
```

Observe

- initContainers
- containers
- status
- volumes

---

# Cleanup

Delete one Pod

```bash
kubectl delete pod multi-container
```

Delete all Pods

```bash
kubectl delete pods --all
```

Delete namespace

```bash
kubectl delete namespace demo
```

---

# Summary

In this lab, you learned:

- Creating Pods
- Multi-container Pods
- Shared networking
- Shared storage using `emptyDir`
- Init Containers
- Multiple Init Containers
- Viewing logs
- Executing commands inside containers
- Pod lifecycle
- Cleanup

---

# Key Observations

### Shared Network

All containers inside a Pod:

- Share the same IP address
- Can communicate using `localhost`

---

### Shared Storage

Containers can exchange files using a shared volume.

---

### Init Container

- Runs before the application
- Must finish successfully
- Executes only once

---

### Multiple Init Containers

Execution order

```
Init 1

↓

Init 2

↓

Application
```

---

### Multi-Container Pod

A Pod may contain:

```
Pod

├── Main Container
├── Sidecar Container
└── Init Container(s)
```

---

# Interview Questions

### Can a Pod contain multiple containers?

Yes.

---

### Do containers inside the same Pod have different IP addresses?

No.

They share the same network namespace and Pod IP.

---

### Can Init Containers run in parallel?

No.

They always execute sequentially.

---

### Can Init Containers restart?

Yes.

If an Init Container fails, Kubernetes restarts it until it completes successfully.

---

### Can containers communicate using localhost?

Yes.

Containers inside the same Pod share the network namespace, so they communicate using `localhost`.

---

# Production Use Cases

- Spring Boot + Fluent Bit
- Spring Boot + Nginx
- Spring Boot + Envoy Proxy
- Database Migration using Init Containers
- Waiting for MySQL before starting the application
- Downloading configuration before application startup