# Kubernetes Network Policies

> **Topic:** Kubernetes Security - Network Policies
>
> **Goal:** Control communication between Pods inside the Kubernetes Cluster.

---

# Table of Contents

1. Introduction
2. Why Network Policies?
3. Default Kubernetes Networking
4. How Network Policies Work
5. Ingress Policy
6. Egress Policy
7. podSelector
8. namespaceSelector
9. ipBlock
10. policyTypes
11. Default Deny
12. Production Architecture
13. Best Practices
14. Interview Questions
15. Complete Practical Lab

---

# What is a Network Policy?

A **NetworkPolicy** is a Kubernetes object that controls network communication between Pods.

It acts like a **Firewall** for Pods.

Without NetworkPolicy

```
Pod A  -------------> Pod B

Pod A  -------------> Database

Pod A  -------------> Redis

Pod A  -------------> Kafka
```

Everything can communicate with everything.

---

# Why Network Policies?

Imagine an E-Commerce application

```
                 Internet

                     |

                 Frontend

                     |

                     ▼

                 Backend

                     |

                     ▼

                 MySQL
```

Suppose Frontend is compromised.

Without Network Policies

```
Frontend

 |

 |---------> Database

 |

 |---------> Redis

 |

 |---------> Kafka

 |

 |---------> Monitoring

 |

 |---------> Every Pod
```

The attacker can move everywhere.

---

With Network Policies

```
Frontend

 |

 ▼

Backend

 |

 ✖ Database Direct Access

 ✖ Kafka

 ✖ Redis

 ✖ Monitoring
```

Now only allowed communication is possible.

---

# Principle of Least Privilege

Pods should communicate only with the services they actually require.

Never allow unrestricted Pod-to-Pod communication.

---

# Default Kubernetes Networking

By default,

```
Every Pod

↓

Can communicate

↓

Every Other Pod
```

There is **NO isolation**.

---

# How Network Policies Work

Network Policies use Labels.

Example

```yaml
labels:
  app: frontend
```

```yaml
labels:
  app: backend
```

```yaml
labels:
  app: mysql
```

Network Policy selects Pods using labels.

---

# Components of Network Policy

```yaml
apiVersion: networking.k8s.io/v1

kind: NetworkPolicy

metadata:

spec:

  podSelector:

  policyTypes:

  ingress:

  egress:
```

---

# podSelector

Determines which Pods are protected.

Example

```yaml
podSelector:

  matchLabels:

    app: backend
```

Only Backend Pods are affected.

---

# policyTypes

Possible values

```yaml
policyTypes:

- Ingress
```

Only Incoming traffic controlled.

---

```yaml
policyTypes:

- Egress
```

Only Outgoing traffic controlled.

---

```yaml
policyTypes:

- Ingress

- Egress
```

Both directions controlled.

---

# Ingress

Controls

> Who can access this Pod?

Example

```
Frontend

↓

Backend

Allowed
```

Database

↓

Backend

Blocked

---

# Egress

Controls

> Where can this Pod connect?

Example

```
Backend

↓

Database

Allowed
```

Backend

↓

Internet

Blocked

---

# namespaceSelector

Allows communication from selected namespaces.

Example

```yaml
namespaceSelector:

  matchLabels:

    env: production
```

---

# ipBlock

Allows traffic from external IP ranges.

Example

```yaml
ipBlock:

  cidr: 10.10.0.0/16
```

---

# Default Deny

The most common production pattern.

```yaml
podSelector: {}

policyTypes:

- Ingress
```

Meaning

```
Nobody

↓

Can communicate

↓

With Any Pod
```

Until explicitly allowed.

---

# Production Example

```
Internet

↓

Ingress

↓

Frontend

↓

Backend

↓

MySQL
```

Allowed

✔ Internet → Ingress

✔ Frontend → Backend

✔ Backend → MySQL

Blocked

✖ Frontend → MySQL

✖ Database → Frontend

✖ Random Pod → Database

---

# Important Requirement

Network Policies work only if your CNI supports them.

Supported CNIs

- Calico
- Cilium
- Antrea

Default Kind CNI (`kindnet`) does **not** enforce Network Policies.

---

# Best Practices

✔ Start with Default Deny

✔ Allow only required communication

✔ Use Labels

✔ Separate namespaces

✔ Restrict database access

✔ Restrict Internet access

✔ Test every policy

---

# Interview Questions

### What is a Network Policy?

A Kubernetes object used to control Pod-to-Pod communication.

---

### Does Kubernetes block communication by default?

No.

All Pods can communicate unless Network Policies are enforced.

---

### Difference between RBAC and Network Policy?

RBAC controls API permissions.

Network Policy controls network communication.

---

### Which component enforces Network Policies?

The CNI Plugin.

Examples

- Calico
- Cilium

---

# ================================
# COMPLETE PRACTICAL LAB
# ================================

# Goal

Create

```
Frontend

↓

Backend

↓

MySQL
```

Then

✔ Verify all Pods communicate

✔ Install Calico

✔ Apply Default Deny

✔ Allow Frontend → Backend

✔ Allow Backend → MySQL

✔ Verify Frontend → MySQL is blocked

---

# Step 1 Delete Cluster

```bash
kind delete cluster --name network-lab
```

---

# Step 2 Create Cluster

```bash
kind create cluster --name network-lab
```

---

Verify

```bash
kubectl cluster-info

kubectl get nodes

kubectl get pods -A
```

Expected

```
kindnet
```

---

# Step 3 Remove kindnet

```bash
kubectl delete daemonset kindnet -n kube-system
```

---

# Step 4 Install Calico

```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.30.3/manifests/calico.yaml
```

Wait

```bash
kubectl get pods -n kube-system -w
```

Verify

```
calico-node

calico-kube-controllers
```

---

# Step 5 Create Namespace

```bash
kubectl create namespace demo
```

---

# Step 6 Frontend Pod

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: frontend
  namespace: demo
  labels:
    app: frontend

spec:
  containers:
  - name: nginx
    image: nginx
```

---

# Step 7 Backend Pod

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: backend
  namespace: demo
  labels:
    app: backend

spec:
  containers:
  - name: nginx
    image: nginx
```

---

# Step 8 MySQL Pod

```yaml
apiVersion: v1
kind: Pod

metadata:
  name: mysql
  namespace: demo
  labels:
    app: mysql

spec:
  containers:
  - name: mysql
    image: mysql:8

    env:
    - name: MYSQL_ROOT_PASSWORD
      value: root
```

---

# Step 9 Apply

```bash
kubectl apply -f frontend.yaml

kubectl apply -f backend.yaml

kubectl apply -f mysql.yaml
```

---

Verify

```bash
kubectl get pods -n demo -o wide
```

---

# Step 10 Create Services

Frontend

```yaml
apiVersion: v1
kind: Service

metadata:
  name: frontend
  namespace: demo

spec:
  selector:
    app: frontend

  ports:
  - port: 80
```

Backend

```yaml
apiVersion: v1
kind: Service

metadata:
  name: backend
  namespace: demo

spec:
  selector:
    app: backend

  ports:
  - port: 80
```

MySQL

```yaml
apiVersion: v1
kind: Service

metadata:
  name: mysql
  namespace: demo

spec:
  selector:
    app: mysql

  ports:
  - port: 3306
```

---

Apply

```bash
kubectl apply -f .
```

---

Verify

```bash
kubectl get svc -n demo
```

---

# Step 11 Install Testing Tools

```bash
kubectl exec -it frontend -n demo -- bash
```

```bash
apt update

apt install curl telnet dnsutils iputils-ping netcat-openbsd -y
```

Repeat for backend.

---

# Step 12 Test Before Policies

DNS

```bash
nslookup backend

nslookup mysql
```

HTTP

```bash
curl http://backend
```

MySQL

```bash
nc -zv mysql 3306
```

Everything should work.

---

# Step 13 Default Deny Policy

```yaml
apiVersion: networking.k8s.io/v1

kind: NetworkPolicy

metadata:
  name: default-deny
  namespace: demo

spec:

  podSelector: {}

  policyTypes:

  - Ingress
```

Apply

```bash
kubectl apply -f default-deny.yaml
```

---

Test

```bash
curl http://backend
```

Fails

```bash
nc -zv mysql 3306
```

Fails

---

# Step 14 Allow Frontend → Backend

```yaml
apiVersion: networking.k8s.io/v1

kind: NetworkPolicy

metadata:
  name: allow-frontend
  namespace: demo

spec:

  podSelector:

    matchLabels:
      app: backend

  ingress:

  - from:

    - podSelector:

        matchLabels:

          app: frontend
```

Apply

```bash
kubectl apply -f allow-frontend.yaml
```

Test

```bash
curl http://backend
```

Works

---

# Step 15 Allow Backend → MySQL

```yaml
apiVersion: networking.k8s.io/v1

kind: NetworkPolicy

metadata:
  name: allow-mysql
  namespace: demo

spec:

  podSelector:

    matchLabels:

      app: mysql

  ingress:

  - from:

    - podSelector:

        matchLabels:

          app: backend

    ports:

    - protocol: TCP
      port: 3306
```

Apply

```bash
kubectl apply -f allow-mysql.yaml
```

---

Test

Backend

```bash
nc -zv mysql 3306
```

Works

Frontend

```bash
nc -zv mysql 3306
```

Fails

---

# Final Communication Matrix

| Source | Destination | Result |
|---------|-------------|--------|
| Frontend | Backend | ✅ |
| Backend | MySQL | ✅ |
| Frontend | MySQL | ❌ |
| MySQL | Backend | ❌ |
| MySQL | Frontend | ❌ |

---

# Useful Commands

```bash
kubectl get networkpolicy -n demo

kubectl describe networkpolicy -n demo

kubectl get pods -n demo --show-labels

kubectl get svc -n demo

kubectl get endpoints -n demo

kubectl logs <pod>

kubectl exec -it frontend -n demo -- bash

kubectl exec -it backend -n demo -- bash
```

---

# Production Learning Outcome

After completing this lab you will understand:

- Why Network Policies are required.
- Why Kind's default `kindnet` CNI is not enough for policy enforcement.
- How to install a CNI (Calico) that supports Network Policies.
- How Pods communicate using Services and Kubernetes DNS.
- How to create and apply a default-deny policy.
- How to selectively allow traffic using Pod labels.
- How to secure microservice communication using the principle of least privilege.
- How to verify policy behavior with `curl`, `nc`, `kubectl describe`, and other troubleshooting commands.