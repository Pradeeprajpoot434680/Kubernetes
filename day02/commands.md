

1. Check kubectl Installation & Version

| Command                         | Description                                                                                                        |
| ------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `which kubectl`                 | Displays the location of the `kubectl` executable to verify it is installed and available in your system's `PATH`. |
| `kubectl version --client`      | Shows only the installed **kubectl client** version.                                                               |
| `kubectl version`               | Displays both the **client** and **server (cluster)** Kubernetes versions.                                         |
| `kubectl version --output=yaml` | Outputs detailed version information in **YAML** format.                                                           |



2. Cluster & Context Management
| Command                                     | Description                                                       |
| ------------------------------------------- | ----------------------------------------------------------------- |
| `kubectl config get-clusters`               | Lists all Kubernetes clusters configured in your kubeconfig file. |
| `kubectl config current-context`            | Shows the currently active Kubernetes context.                    |
| `kubectl config get-contexts`               | Lists all available contexts along with the active one.           |
| `kubectl config use-context <context-name>` | Switches the current context to another cluster/user combination. |



3. View kubeconfig Configuration

| Command                                                                    | Description                                                              |
| -------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| `kubectl config view`                                                      | Displays the complete kubeconfig configuration.                          |
| `kubectl config view --minify`                                             | Shows only the configuration for the currently active context.           |
| `kubectl config view --raw`                                                | Displays the kubeconfig including embedded certificates and credentials. |
| `kubectl config view -o jsonpath='{.clusters[*].name}'`                    | Lists only the configured cluster names.                                 |
| `kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'` | Displays the API server URL of the current cluster.                      |


4. Cluster Information

| Command                 | Description                                                               |
| ----------------------- | ------------------------------------------------------------------------- |
| `kubectl cluster-info`  | Displays the addresses of the Kubernetes control plane and core services. |
| `kubectl api-resources` | Lists all Kubernetes resource types supported by the API server.          |
| `kubectl api-versions`  | Displays all supported Kubernetes API versions.                           |


5. Node Management

| Command                             | Description                                                                                                           |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `kubectl get nodes`                 | Lists all nodes in the cluster.                                                                                       |
| `kubectl get nodes -o wide`         | Shows detailed node information such as IP address, OS, container runtime, and Kubernetes version.                    |
| `kubectl describe node <node-name>` | Displays detailed information about a specific node including capacity, labels, taints, conditions, and running pods. |



6. Namespace Management

| Command                  | Description                             |
| ------------------------ | --------------------------------------- |
| `kubectl get namespaces` | Lists all namespaces in the cluster.    |
| `kubectl get ns`         | Short form of `kubectl get namespaces`. |


7. View Cluster Resources

| Command                            | Description                                                                                       |
| ---------------------------------- | ------------------------------------------------------------------------------------------------- |
| `kubectl get all`                  | Lists common resources (Pods, Services, Deployments, ReplicaSets, etc.) in the current namespace. |
| `kubectl get all -A`               | Lists common resources across all namespaces.                                                     |
| `kubectl get all --all-namespaces` | Same as `-A`; displays resources from every namespace.                                            |


8. Pod Commands


| Command                              | Description                                                         |
| ------------------------------------ | ------------------------------------------------------------------- |
| `kubectl get pods`                   | Lists Pods in the current namespace.                                |
| `kubectl get pods -A`                | Lists Pods in all namespaces.                                       |
| `kubectl get pods -A -o wide`        | Displays Pods with additional details such as node name and pod IP. |
| `kubectl get pod <pod-name> -o yaml` | Outputs the complete Pod definition in YAML format.                 |
| `kubectl get pod <pod-name> -o json` | Outputs the complete Pod definition in JSON format.                 |



9. Workload Resources


| Command                      | Description                              |
| ---------------------------- | ---------------------------------------- |
| `kubectl get deployments -A` | Lists all Deployments across namespaces. |
| `kubectl get rs -A`          | Lists all ReplicaSets across namespaces. |
| `kubectl get svc -A`         | Lists all Services across namespaces.    |
| `kubectl get secrets -A`     | Lists all Secrets across namespaces.     |


10. Logs

| Command                      | Description                              |
| ---------------------------- | ---------------------------------------- |
| `kubectl logs <pod-name>`    | Displays logs from a Pod.                |
| `kubectl logs -f <pod-name>` | Streams logs continuously (follow mode). |




11. Execute Commands Inside a Pod


| Command                               | Description                                                            |
| ------------------------------------- | ---------------------------------------------------------------------- |
| `kubectl exec -it <pod-name> -- bash` | Opens an interactive Bash shell inside the Pod (if Bash is installed). |
| `kubectl exec -it <pod-name> -- sh`   | Opens an interactive Shell (`sh`) inside the Pod.                      |




12. Events

| Command                                                    | Description                                  |
| ---------------------------------------------------------- | -------------------------------------------- |
| `kubectl get events -A`                                    | Displays cluster events from all namespaces. |
| `kubectl get events --sort-by=.metadata.creationTimestamp` | Lists events sorted by creation time.        |


13. Resource Documentation


| Command                    | Description                                             |
| -------------------------- | ------------------------------------------------------- |
| `kubectl explain pod`      | Shows documentation for the Pod resource.               |
| `kubectl explain pod.spec` | Displays documentation for the `spec` section of a Pod. |


