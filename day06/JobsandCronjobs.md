# Kubernetes Jobs and CronJobs - Practical Guide

## 1. Kubernetes Job

## What is a Job?

A Kubernetes **Job** creates a Pod and runs a task until it completes successfully.

Examples:

- Database backup
- Data migration
- Report generation
- Batch processing
- One-time scripts

Flow:

```
Job
 |
 | creates
 ↓
Pod
 |
 | runs task
 ↓
Task completes
 |
 ↓
Pod status = Completed
```

---

# Job Practical Example 1: Hello Kubernetes

## Step 1: Create Job YAML

Create file:

```
job.yaml
```

Content:

```yaml
apiVersion: batch/v1
kind: Job

metadata:
  name: hello-job

spec:
  template:
    spec:

      restartPolicy: Never

      containers:
      - name: hello
        image: busybox
        command:
        - sh
        - -c
        - echo "Hello Kubernetes"
```

---

## Step 2: Create the Job

```bash
kubectl apply -f job.yaml
```

Output:

```
job.batch/hello-job created
```

---

## Step 3: Check Job Status

```bash
kubectl get jobs
```

Example:

```
NAME         COMPLETIONS
hello-job    1/1
```

Meaning:

```
1 Job completed successfully
```

---

## Step 4: Check Pod

```bash
kubectl get pods -o wide
```

Example:

```
NAME              STATUS      NODE
hello-job-x7abc   Completed   kind-worker
```

The NODE column shows where Kubernetes ran the Job.

---

## Step 5: View Logs

Find Pod name:

```bash
kubectl get pods
```

Then:

```bash
kubectl logs <pod-name>
```

Output:

```
Hello Kubernetes
```

---

# Job Scheduling on Nodes

By default Kubernetes decides the node.

Example:

```
Cluster

kind-control-plane

kind-worker
    |
    |
    Job Pod

kind-worker2
```

The scheduler selects a suitable node.

---

# Run Job on Specific Node

## Step 1: Add Node Label

Example:

```bash
kubectl label node kind-worker type=worker
```

Check:

```bash
kubectl get nodes --show-labels
```

---

## Step 2: Add nodeSelector

job.yaml

```yaml
apiVersion: batch/v1
kind: Job

metadata:
  name: node-job

spec:

  template:

    spec:

      nodeSelector:
        type: worker

      restartPolicy: Never

      containers:

      - name: hello
        image: busybox

        command:
        - sh
        - -c
        - echo "Running on worker node"
```

Apply:

```bash
kubectl apply -f job.yaml
```

Check:

```bash
kubectl get pods -o wide
```

Now the Pod runs only on:

```
kind-worker
```

---

# Job Failure and Retry

Example:

```yaml
apiVersion: batch/v1
kind: Job

metadata:
  name: failed-job

spec:

  backoffLimit: 3

  template:

    spec:

      restartPolicy: Never

      containers:

      - name: test
        image: busybox

        command:
        - sh
        - -c
        - exit 1
```

Explanation:

```
Pod fails

↓

Job retries

↓

Maximum 3 attempts

↓

Job marked failed
```

---

# Parallel Jobs

A Job can run multiple Pods.

Example:

```yaml
apiVersion: batch/v1
kind: Job

metadata:
  name: parallel-job

spec:

  completions: 5

  parallelism: 2

  template:

    spec:

      restartPolicy: Never

      containers:

      - name: worker
        image: busybox

        command:
        - sh
        - -c
        - echo "Processing data"
```

Meaning:

```
Need 5 successful executions

Run 2 Pods at the same time
```

---

# Useful Job Commands

## Create Job

```bash
kubectl apply -f job.yaml
```

---

## View Jobs

```bash
kubectl get jobs
```

---

## View Pods

```bash
kubectl get pods
```

---

## Describe Job

```bash
kubectl describe job hello-job
```

---

## View Logs

```bash
kubectl logs <pod-name>
```

---

## Delete Job

```bash
kubectl delete job hello-job
```

---

---

# 2. Kubernetes CronJob

## What is CronJob?

A CronJob creates Jobs automatically according to a schedule.

Examples:

- Daily backup
- Weekly cleanup
- Hourly reports
- Scheduled scripts

Flow:

```
CronJob

    |
    | schedule time reached

    ↓

Job

    ↓

Pod

    ↓

Task completed
```

---

# Cron Schedule Format

Cron has five fields:

```
* * * * *
│ │ │ │ │
│ │ │ │ └── Day of week
│ │ │ └──── Month
│ │ └────── Day of month
│ └──────── Hour
└────────── Minute
```

Examples:

| Schedule | Meaning |
|---|---|
| `* * * * *` | Every minute |
| `*/5 * * * *` | Every 5 minutes |
| `0 * * * *` | Every hour |
| `0 2 * * *` | Daily 2 AM |
| `0 0 * * 0` | Every Sunday |

---

# CronJob Practical Example

## Step 1: Create cronjob.yaml

```yaml
apiVersion: batch/v1
kind: CronJob

metadata:
  name: hello-cron

spec:

  schedule: "*/2 * * * *"

  jobTemplate:

    spec:

      template:

        spec:

          restartPolicy: Never

          containers:

          - name: hello

            image: busybox

            command:

            - sh

            - -c

            - date
```

This runs every 2 minutes.

---

# Step 2: Create CronJob

```bash
kubectl apply -f cronjob.yaml
```

Output:

```
cronjob.batch/hello-cron created
```

---

# Step 3: Check CronJob

```bash
kubectl get cronjobs
```

Example:

```
NAME          SCHEDULE
hello-cron    */2 * * * *
```

---

# Step 4: Wait and Check Jobs

After 2 minutes:

```bash
kubectl get jobs
```

Example:

```
NAME                    COMPLETIONS
hello-cron-292929       1/1
```

Every schedule creates a new Job.

---

# Step 5: Check Pods

```bash
kubectl get pods
```

Example:

```
hello-cron-292929-xabc
Completed
```

---

# Step 6: View Logs

```bash
kubectl logs <pod-name>
```

Example:

```
Fri Jul 10 10:30:00 UTC 2026
```

---

# CronJob History Control

CronJobs keep old Jobs.

Example:

```yaml
spec:

  successfulJobsHistoryLimit: 3

  failedJobsHistoryLimit: 1
```

Meaning:

Keep:

```
Last 3 successful Jobs

Last 1 failed Job
```

---

# Suspend a CronJob

Stop creating new Jobs:

```yaml
spec:

  suspend: true
```

Resume:

```yaml
spec:

  suspend: false
```

---

# Useful CronJob Commands

## Create CronJob

```bash
kubectl apply -f cronjob.yaml
```

---

## View CronJobs

```bash
kubectl get cronjobs
```

---

## View Jobs created by CronJob

```bash
kubectl get jobs
```

---

## View Pods

```bash
kubectl get pods
```

---

## Describe CronJob

```bash
kubectl describe cronjob hello-cron
```

---

## Delete CronJob

```bash
kubectl delete cronjob hello-cron
```

---

# Job vs CronJob

| Job | CronJob |
|---|---|
| Runs once | Runs repeatedly |
| Manual execution | Scheduled execution |
| Creates Pods | Creates Jobs |
| Backup now | Daily backup |
| Migration | Hourly reports |

---

# Complete Kubernetes Workload Comparison

| Resource | Purpose | Example |
|---|---|---|
| Deployment | Long-running applications | Web server |
| DaemonSet | One Pod per node | Monitoring agent |
| Job | One-time task | Database migration |
| CronJob | Scheduled tasks | Daily backup |

---

# Practice Tasks

## Task 1

Create a Job that:

- Uses busybox
- Prints hostname
- Completes successfully


## Task 2

Create a Job that:

- Runs 5 successful completions
- Runs 2 Pods simultaneously


## Task 3

Create a CronJob that:

- Runs every minute
- Prints current date


## Task 4

Create a CronJob that:

- Runs a backup script every day at 2 AM


# Important Concepts Learned

- Job creates Pods for one-time tasks.
- CronJob creates Jobs on a schedule.
- Scheduler decides which node runs the Pod.
- nodeSelector can control node placement.
- restartPolicy for Jobs is usually Never or OnFailure.
- Completed Pods remain until cleaned up.
- CronJobs automate repeated workloads.