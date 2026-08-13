# ArgoCD GitOps

These manifests add the repo-owned part of the GitOps model:

- one ArgoCD `AppProject` for the platform
- one `ApplicationSet` that fans the shared Helm chart out per service and per operator

Update the placeholder `repoURL` and destination cluster/server values before applying them to
your ArgoCD control plane.
