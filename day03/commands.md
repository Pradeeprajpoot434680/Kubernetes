# Kubernetes Deployments Notes

## 1. Create a Kind Cluster

```bash
kind create cluster --name demo
```

Verify:

```bash
kubectl cluster-info
kubectl get nodes
```

---

## 2. Create Deployment YAML

```yaml
apiVersion: apps/v1
kind: Deployment

metadata:
  name: spring-app

spec:
  replicas: 3

  selector:
    matchLabels:
      app: spring

  template:
    metadata:
      labels:
        app: spring

    spec:
      containers:
      - name: spring
        image: nginx:1.25
```

Apply it:

```bash
kubectl apply -f deployment.yaml
```

---

## 3. Explain Deployment

```bash
kubectl explain deployment
kubectl explain deployment.spec
kubectl explain deployment.spec.template
kubectl explain deployment --recursive
```

---

## 4. Create Deployment

```bash
kubectl apply -f deployment.yaml
```

---

## 5. View Deployments

```bash
kubectl get deployments
kubectl get deploy -o wide
kubectl get deploy spring-app -o yaml
kubectl get deploy spring-app -o json
```

---

## 6. Describe Deployment

```bash
kubectl describe deployment spring-app
```

---

## 7. ReplicaSets

```bash
kubectl get rs
kubectl describe rs
```

---

## 8. Pods

```bash
kubectl get pods
kubectl get pods -o wide
kubectl get pods -w
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

---

## 9. Scale Deployment

Increase replicas:

```bash
kubectl scale deployment spring-app --replicas=5
```

Decrease replicas:

```bash
kubectl scale deployment spring-app --replicas=2
```

---

## 10. Edit Deployment

```bash
kubectl edit deployment spring-app
```

---

## 11. Update Image

```bash
kubectl set image deployment/spring-app spring=nginx:1.26
```

---

## 12. Rollout Status

```bash
kubectl rollout status deployment spring-app
```

Shows the progress of creating or updating Pods.

---

## 13. Rollout History

```bash
kubectl rollout history deployment spring-app
```

Shows Deployment revisions.

---

## 14. Rollback

Undo the latest rollout:

```bash
kubectl rollout undo deployment spring-app
```

Undo to a specific revision:

```bash
kubectl rollout undo deployment spring-app --to-revision=2
```

---

## 15. Restart Deployment

```bash
kubectl rollout restart deployment spring-app
```

Restarts all Pods without changing the YAML.

---

## 16. Pause and Resume Rollout

Pause:

```bash
kubectl rollout pause deployment spring-app
```

Resume:

```bash
kubectl rollout resume deployment spring-app
```

Useful when making multiple Deployment changes before applying them together.

---

## 17. Delete Deployment

```bash
kubectl delete deployment spring-app
```

or

```bash
kubectl delete -f deployment.yaml
```

---

## 18. Validate YAML

Client-side validation:

```bash
kubectl apply -f deployment.yaml --dry-run=client
```

Checks YAML locally without contacting the cluster.

Server-side validation:

```bash
kubectl apply -f deployment.yaml --dry-run=server
```

Checks the manifest against the Kubernetes API server without creating resources.

---

## 19. Export Live Configuration

```bash
kubectl get deployment spring-app -o yaml > deployment-live.yaml
```

---

## 20. Delete All Resources

```bash
kubectl delete all --all
```

Deletes all Pods, Services, Deployments, ReplicaSets, etc., in the current namespace.

---

# Deployment Architecture

```
Deployment
      │
      ▼
ReplicaSet
      │
      ▼
Pods
      │
      ▼
Containers
```

- **Deployment**: Manages application updates and desired state.
- **ReplicaSet**: Maintains the required number of Pod replicas.
- **Pod**: Runs one or more containers.
- **Container**: The actual application process (for example, NGINX).

# Most Common Commands

```bash
kubectl apply -f deployment.yaml
kubectl get deploy
kubectl get rs
kubectl get pods
kubectl describe deployment spring-app
kubectl scale deployment spring-app --replicas=5
kubectl edit deployment spring-app
kubectl set image deployment/spring-app spring=nginx:1.26
kubectl rollout status deployment spring-app
kubectl rollout history deployment spring-app
kubectl rollout restart deployment spring-app
kubectl rollout undo deployment spring-app
kubectl delete deployment spring-app
```