# GitLab Settings Runbook

These controls are part of the target delivery model, but they are administered in GitLab and
cannot be enforced from source code alone.

## Apply in GitLab

1. Protect `main` and release branches.
2. Require merge requests with at least one approval.
3. Require pipeline success before merge.
4. Enforce signed commits if your GitLab tier supports it.
5. Set the default merge request template to `.gitlab/merge_request_templates/Default.md`.
6. Create issue boards backed by the feature and bug templates in this folder.
7. Wire deployment environments so issues, merge requests, and deploys cross-link automatically.

## Repo-owned counterparts already present

- Jenkins pipeline gates in `Jenkinsfile`
- Helm, GitOps, and rollout manifests under `deploy/`
- QA automation suites under `qa-automation/`
- MCP workspace config in `.vscode/mcp.json`
