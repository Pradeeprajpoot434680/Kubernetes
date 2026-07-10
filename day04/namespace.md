# Kubernetes Namespaces - Complete Notes

> Beginner to Advanced Guide with Concepts, Commands, Examples, Internal Working, and Best Practices

---

# Table of Contents

1. Introduction
2. Why Kubernetes Needs Namespaces
3. What is a Namespace?
4. Kubernetes Architecture
5. Default Namespaces
6. Creating Namespaces
7. Listing Namespaces
8. Switching Namespace
9. Namespace YAML
10. Deploying Resources
11. Namespace Scope
12. DNS in Namespaces
13. Resource Quotas
14. LimitRange
15. RBAC
16. Network Policies
17. Deleting Namespaces
18. Useful Commands
19. Best Practices
20. Interview Questions
21. Cheat Sheet

---

# 1. Introduction

A **Namespace** is a logical partition inside a Kubernetes cluster.

It helps divide a single cluster into multiple virtual clusters.

Example:

```
Cluster

├── frontend
├── backend
├── testing
└── production
```

Each namespace can contain its own Pods, Services, Deployments, Secrets, ConfigMaps, etc.

---

# 2. Why Kubernetes Needs Namespaces

Imagine three teams.

```
Frontend Team
Backend Team
QA Team
```

Each team wants a Deployment named:

```
my-app
```

Without namespaces

```
Cluster

my-app
my-app
my-app
```

This creates name conflicts.

With namespaces

```
frontend/my-app

backend/my-app

testing/my-app
```

No conflicts occur because the namespace is part of the resource identity.

---

# 3. What is a Namespace?

A namespace is a logical boundary that isolates resources.

Namespace-scoped resources include:

- Pods
- Deployments
- ReplicaSets
- StatefulSets
- Services
- ConfigMaps
- Secrets
- Jobs
- CronJobs
- PVCs

Cluster-scoped resources include:

- Nodes
- PersistentVolumes
- StorageClasses
- ClusterRoles
- ClusterRoleBindings
- Namespaces themselves

---

# 4. Kubernetes Architecture

```
               User
                 |
             kubectl
                 |
           API Server
                 |
      ---------------------
      |         |         |
 Scheduler Controller etcd
```

Namespaces are stored as Kubernetes objects in **etcd**.

---

# 5. Default Namespaces

## default

Default namespace for user workloads.

```
kubectl get pods
```

---

## kube-system

Contains Kubernetes control plane components.

Example

```
coredns

kube-proxy

etcd

scheduler
```

---

## kube-public

Readable by all users.

---

## kube-node-lease

Stores Node lease information for heartbeat management.

---

# 6. Create Namespace

```
kubectl create namespace frontend
```

Short form

```
kubectl create ns frontend
```

Verify

```
kubectl get ns
```

---

# 7. List Namespaces

```
kubectl get namespaces
```

or

```
kubectl get ns
```

Detailed

```
kubectl get ns -o wide
```

YAML

```
kubectl get ns frontend -o yaml
```

JSON

```
kubectl get ns frontend -o json
```

Describe

```
kubectl describe ns frontend
```

---

# 8. Namespace YAML

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: frontend
```

Create

```
kubectl apply -f namespace.yaml
```

Delete

```
kubectl delete -f namespace.yaml
```

---

# 9. Create Pod in Namespace

```
kubectl run nginx \
--image=nginx \
-n frontend
```

Check

```
kubectl get pods -n frontend
```

---

# 10. Create Deployment

```
kubectl create deployment nginx \
--image=nginx \
-n frontend
```

Scale

```
kubectl scale deployment nginx \
--replicas=5 \
-n frontend
```

---

# 11. Namespace in YAML

```yaml
metadata:
  name: nginx
  namespace: frontend
```

Deploy

```
kubectl apply -f deployment.yaml
```

---

# 12. Services

Create

```
kubectl expose deployment nginx \
--port=80 \
--target-port=80 \
-n frontend
```

List

```
kubectl get svc -n frontend
```

---

# 13. DNS

Service Name

```
nginx
```

Inside same namespace

```
http://nginx
```

Different namespace

```
http://nginx.backend
```

Full DNS

```
http://nginx.backend.svc.cluster.local
```

---

# 14. Switch Default Namespace

Current Context

```
kubectl config current-context
```

Set Namespace

```
kubectl config set-context --current --namespace=frontend
```

Verify

```
kubectl config view --minify
```

---

# 15. Delete Namespace

```
kubectl delete ns frontend
```

Everything inside gets deleted.

---

# 16. ResourceQuota

Example

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute
  namespace: frontend

spec:
  hard:
    pods: "10"
    requests.cpu: "4"
    requests.memory: 8Gi
```

Apply

```
kubectl apply -f quota.yaml
```

---

# 17. LimitRange

```yaml
apiVersion: v1
kind: LimitRange

metadata:
  name: limits
  namespace: frontend

spec:
  limits:
  - default:
      cpu: 500m
      memory: 512Mi

    defaultRequest:
      cpu: 200m
      memory: 256Mi

    type: Container
```

---

# 18. RBAC

Role

```yaml
kind: Role
```

RoleBinding

```yaml
kind: RoleBinding
```

Example

```
kubectl create role pod-reader \
--verb=get,list,watch \
--resource=pods \
-n frontend
```

---

# 19. Network Policy

Restrict communication between namespaces.

Example

```
frontend

↓

backend

Allowed

↓

database

Denied
```

---

# 20. Internal Working

When you execute

```
kubectl apply -f deployment.yaml
```

Flow

```
kubectl

↓

API Server

↓

Authentication

↓

Authorization

↓

Namespace Exists?

↓

Store in etcd

↓

Controller

↓

ReplicaSet

↓

Pods

↓

Scheduler

↓

Worker Node
```

---

# 21. Common Commands

## List namespaces

```
kubectl get ns
```

## Describe namespace

```
kubectl describe ns frontend
```

## Delete namespace

```
kubectl delete ns frontend
```

## Create namespace

```
kubectl create ns frontend
```

## Pods

```
kubectl get pods -n frontend
```

## Services

```
kubectl get svc -n frontend
```

## Deployments

```
kubectl get deploy -n frontend
```

## Secrets

```
kubectl get secrets -n frontend
```

## ConfigMaps

```
kubectl get configmap -n frontend
```

## Events

```
kubectl get events -n frontend
```

## Logs

```
kubectl logs pod-name -n frontend
```

## Exec

```
kubectl exec -it pod-name -n frontend -- bash
```

## Port Forward

```
kubectl port-forward pod-name 8080:80 -n frontend
```

---

# 22. Best Practices

- Use one namespace per application or team.
- Do not deploy user applications in `kube-system`.
- Use ResourceQuota for resource control.
- Apply RBAC to restrict access.
- Use NetworkPolicies for traffic isolation.
- Label namespaces for organization.
- Use meaningful namespace names.

---

# 23. Interview Questions

### What is a namespace?

A logical partition inside a Kubernetes cluster.

---

### Are namespaces physical?

No.

They are logical isolation.

---

### Which resources are not namespaced?

- Nodes
- PersistentVolumes
- StorageClasses
- Namespaces
- ClusterRoles
- ClusterRoleBindings

---

### Can two namespaces contain Pods with the same name?

Yes.

Example

```
frontend/nginx

backend/nginx
```

---

### Which namespace contains Kubernetes components?

```
kube-system
```

---

# 24. Cheat Sheet

```
kubectl get ns

kubectl create ns dev

kubectl delete ns dev

kubectl get pods -n dev

kubectl get svc -n dev

kubectl describe ns dev

kubectl config set-context --current --namespace=dev

kubectl apply -f namespace.yaml

kubectl get deploy -n dev

kubectl logs pod-name -n dev

kubectl exec -it pod-name -n dev -- bash
```

---

# Summary

Namespaces provide:

- Logical isolation
- Name separation
- Team separation
- Resource management
- Security boundaries with RBAC
- Quota enforcement
- Easier management of large clusters

They are one of the foundational concepts for organizing workloads in Kubernetes.