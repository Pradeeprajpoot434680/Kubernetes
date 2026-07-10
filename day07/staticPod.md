# Kubernetes Static Pods - Complete Practical Notes

# 1. What is a Static Pod?

A **Static Pod** is a Pod that is created and managed directly by the **kubelet** on a specific node.

The kubelet watches a directory containing Pod YAML files. When it finds a YAML file, it automatically creates the Pod.

Normal Pod flow:

```
kubectl
   |
   |
API Server
   |
   |
Scheduler
   |
   |
kubelet
   |
   |
Pod
```

Static Pod flow:

```
Static Pod YAML file

        |
        |

     kubelet

        |
        |

      Pod
```

---

# 2. Static Pod vs Normal Pod

| Normal Pod | Static Pod |
|---|---|
| Created using kubectl | Created from YAML file on node |
| Controlled by API Server | Controlled by kubelet |
| Stored in etcd | Not stored directly in etcd |
| Scheduler assigns node | Runs only on the node containing YAML |
| Managed by controllers | Managed by kubelet |

---

# 3. Static Pod Location

In kubeadm clusters:

```
/etc/kubernetes/manifests/
```

Check:

```bash
ls /etc/kubernetes/manifests/
```

Example output:

```
etcd.yaml

kube-apiserver.yaml

kube-controller-manager.yaml

kube-scheduler.yaml
```

---

# 4. Important Concept

The YAML file is the source of truth.

Example:

```
/etc/kubernetes/manifests/nginx.yaml

              |
              |
              v

            kubelet

              |
              |
              v

        nginx Static Pod
```

If you modify the YAML:

```
kubelet detects change

        |

restarts/recreates Pod
```

---

# 5. Who Creates Kubernetes Control Plane Pods?

A common question:

## Who creates kube-apiserver Pod?

Answer:

```
kubelet
```

Not:

```
Controller Manager ❌
Scheduler ❌
API Server ❌
```

---

# 6. Control Plane Static Pods

In kubeadm clusters:

```
/etc/kubernetes/manifests/

        |
        |
        +----------------+
        |
        |
        v

kube-apiserver.yaml

        |
        |
        v

kube-apiserver Pod
```

Same for:

```
etcd.yaml

kube-scheduler.yaml

kube-controller-manager.yaml
```

---

# 7. Control Plane Architecture

```
                 kubelet

                    |

        /etc/kubernetes/manifests/

                    |

        ---------------------------

        |            |            |

        v            v            v

   etcd Pod    API Server    Scheduler

                    |

                    |

        Controller Manager
```

All these Pods are Static Pods.

---

# 8. Why Use Static Pods for Control Plane?

Kubernetes has a bootstrap problem.

Question:

Who creates the Controller Manager Pod?

Normally:

```
Controller Manager creates Pods
```

But:

```
Controller Manager itself is a Pod
```

So who creates it first?

Answer:

```
kubelet + Static Pod
```

Static Pods allow Kubernetes to start itself.

---

# 9. Static Pods in KIND Cluster

In a KIND cluster:

```
Laptop

   |

 Docker

   |

 -------------------------

 |          |             |

control   worker      worker2

plane
```

The Kubernetes nodes are Docker containers.

The Static Pod files are inside:

```
kind-control-plane container
```

Not your laptop.

---

# 10. Enter KIND Control Plane Node

Check containers:

```bash
docker ps
```

Example:

```
kind-control-plane

kind-worker

kind-worker2
```

Enter control plane:

```bash
docker exec -it kind-control-plane bash
```

Now:

```
root@kind-control-plane:#
```

---

# 11. Check Static Pod Files

Inside control-plane:

```bash
ls /etc/kubernetes/manifests/
```

Output:

```
etcd.yaml

kube-apiserver.yaml

kube-controller-manager.yaml

kube-scheduler.yaml
```

---

# 12. Check kubelet Static Pod Configuration

Run:

```bash
ps aux | grep kubelet
```

Look for:

```
--pod-manifest-path=/etc/kubernetes/manifests
```

Meaning:

```
kubelet watches this directory
```

---

# 13. Create Your Own Static Pod

## Step 1

Enter control-plane:

```bash
docker exec -it kind-control-plane bash
```

---

## Step 2

Go to manifest directory:

```bash
cd /etc/kubernetes/manifests/
```

---

## Step 3

Create YAML

```bash
vi nginx-static.yaml
```

Add:

```yaml
apiVersion: v1

kind: Pod

metadata:
  name: nginx-static

spec:

  containers:

  - name: nginx

    image: nginx
```

Save.

---

# 14. Check Static Pod

Exit:

```bash
exit
```

Check:

```bash
kubectl get pods
```

Example:

```
nginx-static-kind-control-plane
```

The node name is added automatically.

---

# 15. Delete Static Pod

Try:

```bash
kubectl delete pod nginx-static-kind-control-plane
```

The Pod disappears.

But kubelet sees:

```
nginx-static.yaml still exists
```

So:

```
kubelet

   |

creates Pod again
```

---

# 16. Permanently Remove Static Pod

Remove YAML file:

```bash
rm /etc/kubernetes/manifests/nginx-static.yaml
```

Now kubelet removes the Pod.

---

# 17. Static Pod Failure Handling

Example:

```
kube-apiserver Pod crashes

        |

        v

kubelet detects failure

        |

        v

creates new container
```

No Deployment.

No ReplicaSet.

Only kubelet.

---

# 18. Check Static Pods

All Kubernetes control plane Pods:

```bash
kubectl get pods -n kube-system
```

Example:

```
etcd-kind-control-plane

kube-apiserver-kind-control-plane

kube-controller-manager-kind-control-plane

kube-scheduler-kind-control-plane
```

---

# 19. Important Commands

## List Kubernetes system Pods

```bash
kubectl get pods -n kube-system
```

---

## Enter KIND control plane

```bash
docker exec -it kind-control-plane bash
```

---

## View Static Pod manifests

```bash
ls /etc/kubernetes/manifests/
```

---

## View kubelet configuration

```bash
ps aux | grep kubelet
```

---

## Describe Static Pod

```bash
kubectl describe pod <pod-name>
```

---

# 20. Static Pod Lifecycle

```
YAML File Created

        |

        v

kubelet detects file

        |

        v

Pod Created

        |

        v

Container Starts

        |

        v

Pod Runs
```

If YAML changes:

```
YAML modified

        |

        v

kubelet detects change

        |

        v

Pod recreated
```

---

# 21. Final Comparison

| Feature | Static Pod | Deployment |
|---|---|---|
| Managed by | kubelet | Controller Manager |
| Created by | YAML file on node | API Server |
| Scheduling | Fixed node | Scheduler |
| Replica management | No | Yes |
| Used for | Control plane | Applications |
| Stored in etcd | No direct object | Yes |

---

# Key Points to Remember

1. Static Pods are managed by kubelet.
2. YAML files live in `/etc/kubernetes/manifests/`.
3. Control plane components in kubeadm are Static Pods.
4. kube-apiserver, etcd, scheduler, and controller-manager are created by kubelet.
5. Deleting a Static Pod using kubectl does not permanently remove it.
6. Removing the YAML file removes the Static Pod.
7. Static Pods solve the Kubernetes bootstrap problem.
8. KIND clusters store Static Pod files inside the control-plane container.