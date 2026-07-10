| Command                                  | Purpose                                |
| ---------------------------------------- | -------------------------------------- |
| `kubectl apply -f replicaset.yaml`       | Create/update ReplicaSet               |
| `kubectl get rs`                         | List ReplicaSets                       |
| `kubectl describe rs nginx-rs`           | Show ReplicaSet details                |
| `kubectl get pods`                       | List Pods                              |
| `kubectl get pods -o wide`               | Show Pods and their nodes              |
| `kubectl describe pod <pod-name>`        | Show Pod details                       |
| `kubectl logs <pod-name>`                | View container logs                    |
| `kubectl delete pod <pod-name>`          | Delete a Pod (ReplicaSet recreates it) |
| `kubectl scale rs nginx-rs --replicas=5` | Change the number of Pods              |
| `kubectl delete rs nginx-rs`             | Delete the ReplicaSet                  |
