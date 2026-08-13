# Observability Assets

The codebase already exposes Prometheus metrics and OTLP tracing hooks through
`platform-common`. This folder adds the repo-owned observability assets that sit around those
hooks.

## Included

- `grafana/selfcare-platform-overview.json`
  Import into Grafana for a starter service-health dashboard.
- `../helm/microservice-chart/templates/servicemonitor.yaml`
  Scrapes `/actuator/prometheus` for every service.
- `../helm/microservice-chart/templates/prometheusrule.yaml`
  Lets each service ship alert rules with its Helm values.

## External platform pieces

- Prometheus, Grafana, Jaeger, and Sentry instances still need to exist in the cluster or the
  central platform namespace.
- ArgoCD and Argo Rollouts controllers must be installed cluster-wide before the GitOps and
  rollout manifests in this repo can be applied.
