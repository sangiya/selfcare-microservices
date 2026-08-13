# Performance Automation

This directory holds the k6 performance/load suite that the Jenkins `Performance Test (Dev)`
stage runs after deploy + QA automation.

## What it covers

- gateway health/readiness latency
- content catalogue reads
- loyalty balance reads
- notification list reads
- reports request submission and lookup

The suite deliberately mixes read-heavy traffic with a light async-write path so it exercises the
same public API shell the REST-Assured and Karate suites validate functionally.

## Run locally

From the repo root, after the app stack is up:

```bash
docker compose --profile app up -d --build
docker run --rm \
  --add-host host.docker.internal:host-gateway \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e TENANT_ID=acme-telecom \
  -v "$PWD/qa-automation/performance":/work \
  -w /work \
  grafana/k6:latest \
  run --summary-export=results/k6-summary.json gateway-load.js
```

Open the generated summary JSON at `qa-automation/performance/results/k6-summary.json`.

## Thresholds

- `http_req_failed`: under 1%
- global `http_req_duration p(95)`: under 1s
- per-journey p(95): under 1.2s

These are pragmatic dev-environment gates, not production SLOs.
