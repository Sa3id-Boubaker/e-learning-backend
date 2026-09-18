# OMARISE — Kubernetes manifests (Minikube, Phase 4.2)

This folder contains the Kubernetes manifests for deploying OMARISE to a local
Minikube cluster. **Nothing here has been applied** — these files are meant to be
reviewed, then applied manually (Phase 4.3).

## 1. Architecture

```
Browser
  |
  v
frontend (NodePort, nginx)
  |  /api/*  (nginx.conf, unchanged)
  v
api-gateway (ClusterIP, :8080)
  |  routes via Eureka (lb://SERVICE-NAME)
  v
user-service / course-service / training-service / forum-service / notification-service
(all ClusterIP, all register with eureka-server)
  |                                    |
  v                                    v
MongoDB Atlas (external, unchanged)   rabbitmq (ClusterIP, in-cluster)
```

Eureka (`eureka-server`, ClusterIP :8761) is kept as-is — service discovery is not
replaced with Kubernetes-native mechanisms. `api-gateway` is the only entry point
into the backend; every business service stays ClusterIP-only and is never exposed
directly. RabbitMQ now runs inside Minikube (it ran as a plain Docker container
before). MongoDB stays external on Atlas — no Mongo Deployment/PVC exists here.

## 2. Required files

All 26 files under `k8s/` (namespace, ConfigMap, 5 Secret examples + their README,
9 service folders each with `deployment.yaml` + `service.yaml`) are required for a
working deployment, except the two READMEs which are documentation only.

## 3. Which files are examples only

Everything under `k8s/config/secrets/*.secret.example.yaml` is a template with the
placeholder `CHANGE_ME` — see `k8s/config/secrets/README.md`. **Do not `kubectl apply`
these files as-is.** Copy each one, replace `CHANGE_ME` with your real value, and
apply the copy — ideally without ever committing the copy to git (see section 6).

## 4. Real secrets must never be committed

None of the 5 `.secret.example.yaml` files, the ConfigMap, or any Deployment contain
a real credential, PAT, or token — every value here is either a placeholder
(`CHANGE_ME`), a non-secret default already present in the application's own code, or
the fixed literal `TAG_TO_REPLACE` for image tags. This was verified file by file
during generation (see the accompanying Phase 4.2 report's SECURITY REVIEW section).

## 5. Creating `ghcr-pull-secret` manually

Every Deployment that pulls one of the 8 private GHCR images references
`imagePullSecrets: [{name: ghcr-pull-secret}]`. Create it yourself, once, directly
with `kubectl` — never write the token to a file in this repository:

```
kubectl create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io \
  --docker-username=<your-github-username> \
  --docker-password=<your-PAT-with-read:packages-scope> \
  --docker-email=<your-email> \
  --namespace=omarise
```

## 6. Creating the real application Secrets manually

For each `*.secret.example.yaml` file: copy it (outside git, or to a path covered by
`.gitignore`), replace every `CHANGE_ME` with the real value, then apply the copy —
or skip the file entirely and use `kubectl create secret generic ... --from-literal=...`
directly on the command line, which never touches disk as a YAML file at all.

Real values come from:
- `omarise-mongo` — your MongoDB Atlas database user (same one already in Jenkins'
  `mongo-db-user` / `mongo-db-password` credentials).
- `omarise-jwt` — the same value as Jenkins' `jwt-secret` credential.
- `omarise-mail` — the same value as Jenkins' `mail-username` / `mail-password`.
- `omarise-cloudinary` — the same values as Jenkins' `cloudinary-*` credentials.
- `omarise-rabbitmq` — **new** for Kubernetes: pick any username/password you want
  for the in-cluster broker (see the note inside `rabbitmq.secret.example.yaml` for
  why this differs from the current Docker Compose setup, which relies on RabbitMQ's
  default `guest`/`guest` account).

## 7. Replacing `TAG_TO_REPLACE`

Every `deployment.yaml` under the 8 GHCR-backed services uses the literal image tag
`TAG_TO_REPLACE`, e.g.:

```
image: ghcr.io/sa3id-boubaker/omarise-user-service:TAG_TO_REPLACE
```

Before applying, replace `TAG_TO_REPLACE` with a real, validated Jenkins
`${BUILD_NUMBER}` (the same number visible in the Jenkins build history / on the
GHCR package's version list) — for example `28`. Do this for all 8 files; a single
find-and-replace across `k8s/*/deployment.yaml` for the exact string
`TAG_TO_REPLACE` is enough, since it never appears anywhere else in these files.

**This manual substitution is only needed for the very first bootstrap apply of a
given Deployment** (or if a Deployment is ever deleted and recreated from these
files). Once a Deployment exists in the cluster, routine image updates are handled
automatically by Jenkins — see section 10 — which never edits these files; the
literal tag committed here simply reflects whatever was last applied manually and
is not kept in sync automatically.

## 8. Suggested apply order (Phase 4.3 — not run in this phase)

1. `namespace.yaml`
2. `config/configmap.yaml`
3. The 5 real Secrets (from the examples, with real values — section 6)
4. `ghcr-pull-secret` (section 5)
5. `eureka-server/` (deployment + service)
6. `rabbitmq/` (deployment + service)
7. Business services: `user-service/`, `course-service/`, `training-service/`,
   `forum-service/`, `notification-service/` (any order — they retry Eureka
   registration and RabbitMQ connections on their own; no explicit wait needed)
8. `api-gateway/`
9. `frontend/`

This order isn't strictly required for correctness (every client-side component
retries on failure — see `k8s/README.md` section 9), but it avoids a burst of
harmless "connection refused" log noise on first boot.

## 9. Validation commands (do not run automatically)

After manually applying, from *outside* this repository's automation:

```
kubectl get all -n omarise
kubectl get pods -n omarise -w
kubectl describe pod <pod-name> -n omarise
kubectl logs <pod-name> -n omarise
kubectl get svc frontend -n omarise   # to find the assigned NodePort
minikube service frontend -n omarise --url
```

None of these were run as part of generating these manifests.


## 10. Ongoing deployments via Jenkins CI/CD (added after Phase 5, CI/CD integration)

As of the Jenkins Kubernetes integration, **routine deployments no longer go through
this `k8s/` folder at all** — sections 5–8 above describe the one-time manual
bootstrap (first-ever apply of each resource on a fresh cluster), not the normal
day-to-day flow.

Both `Jenkinsfile`s (`omarise-backend` and `omarise-frontend`) now end with two
stages, run only after `Docker Push` succeeds:

- **Kubernetes Deploy** — for each image this build just pushed, runs
  `kubectl set image deployment/<name> <name>=ghcr.io/sa3id-boubaker/omarise-<name>:${BUILD_NUMBER} -n omarise`.
  This patches only the image field of the already-existing Deployment directly
  from the registry push that just succeeded in that same build — it never runs
  `kubectl apply -f k8s/...` and never touches these YAML files.
- **Kubernetes Rollout Verification** — `kubectl rollout status deployment/<name> -n omarise --timeout=180s`
  for each of those Deployments; a failure here fails the Jenkins build and prints
  targeted diagnostics (`kubectl get pods` / `describe` / `logs --tail=100`) for
  only the failed component.

Backend and frontend are on independent Jenkins `${BUILD_NUMBER}` sequences, so
each pipeline only ever updates the Deployment(s) whose image it just built and
pushed itself: the backend pipeline updates `eureka-server`, `user-service`,
`course-service`, `training-service`, `forum-service`, `notification-service` and
`api-gateway` (in that order); the frontend pipeline updates only `frontend`.
`rabbitmq` is not built by either pipeline and is never touched by these stages —
it stays exactly as applied manually in section 8.

Both pipelines authenticate to this Minikube cluster via a Jenkins **Secret file**
credential (ID `kubeconfig-minikube`, a kubeconfig with its `server:` pointed at
`https://minikube:8443` — reachable because the Jenkins container is attached to
the same Docker network as the `minikube` container, see `jenkins/docker-compose.jenkins.yml`).
The credential's contents are never written to this repository and never printed
in Jenkins logs.

**When you'd still use sections 5–8 of this README**: only when bootstrapping a
brand-new cluster/namespace from scratch, or recreating a Deployment that was
deleted outright. For every other change, just merge to the branch Jenkins builds
from — the CI/CD pipeline handles the rest.
