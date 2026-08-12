# Selfcare Platform -- Reusable Microservices Monorepo

Reference implementation of the target architecture in **Doc 1 (Master Architecture)** and
**Doc 5 (90-Day Pilot)**: a shared multi-tenant platform framework, plus the four Doc 5 pilot
microservices, built once and deployable independently per operator (Doc 1 sec 5).

## What's actually implemented here

| Module | Status | What it is |
|---|---|---|
| `platform-common` | **Complete** | Shared Spring Boot starter every service depends on: tenant resolution, the API Adapter Registry (Doc 1 sec 6.3), feature flags, standard error envelope, correlation IDs, JWT security config. |
| `config-tenant-service` | **Complete** | Doc 1 sec 6: resolves operator identity, serves theme/layout/widget config from MongoDB, Redis-cached. |
| `api-gateway` | **Complete** | Single entry point (Doc 1 sec 4); strangler-fig proxies anything not yet migrated to the legacy PHP app (Doc 1 sec 8). |
| `loyalty-service` | **Complete reference implementation** | Full port of `scapp/StarPointsController.php` (Doc 1 sec 2.3 audit) -- real business logic, real MIFE adapter, real audit trail, real Kafka events, unit + Testcontainers integration tests. **Read this module first** -- every other service should follow its layering. |
| `reports-service`, `notification-service`, `content-service` | **Starter modules** | Same layering/framework wiring as loyalty-service, but the actual per-controller business logic is a TODO -- see each module's `README-TODO.md` for the real, audited controller list to port, ranked by size. `notification-service` additionally has a **working Kafka consumer** wired to loyalty-service's events, so you can see the event-driven pattern end to end even before its own business logic is ported. |

This matches the scope chosen when this codebase was generated: one full reference service plus
a reusable framework, rather than guessing the business logic for all 79 pilot-domain
controllers blind. Extending the starters is exactly the loyalty-service pattern, repeated.

## How the pieces fit together

```
Client (RN app / web-view)
        │
        ▼
   api-gateway  ──(unmigrated paths)──▶ legacy PHP Yii (strangler-fig fallback)
        │
        ├──▶ config-tenant-service  (theme/layout/feature-flag resolution, Doc 1 sec 6)
        ├──▶ loyalty-service        (Doc 5 pilot domain 1/4 -- full reference)
        ├──▶ reports-service        (Doc 5 pilot domain 2/4 -- starter)
        ├──▶ notification-service   (Doc 5 pilot domain 3/4 -- starter, consumes Kafka events)
        └──▶ content-service        (Doc 5 pilot domain 4/4 -- starter)

Kafka: loyalty-service --publishes--> notification-service (already wired, see LoyaltyEventListener)
```

Every service depends on `platform-common`, which is what makes the following true without any
per-service boilerplate:

- **Tenant resolution** (`TenantContext`) -- works whether a service is deployed one-tenant-per-cluster
  (the normal case, Doc 1 sec 5) or behind a shared gateway resolving tenant per request.
- **The API Adapter Registry** -- onboarding an operator whose core/third-party systems differ
  (Doc 1 sec 6.3) means writing one new adapter class, not branching business logic. See
  `loyalty-service`'s `LoyaltyCoreAdapter`/`MifeLoyaltyCoreAdapter` for the full pattern.
- **Feature flags** (Unleash) -- every flag check is tenant-aware by default.
- **A standard error envelope + correlation IDs** -- every service returns the same JSON error
  shape, and every log line is traceable to a request and an operator.

## Running locally

```bash
# Infra only (MySQL, Mongo, Redis, Kafka, Unleash, WireMock MIFE stub):
docker compose up -d

# Infra + every service, built from source:
docker compose --profile app up -d --build
```

Then, e.g.:
```bash
curl -X POST http://localhost:8080/api/v1/loyalty/register \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-Id: acme-telecom' \
  -d '{"nationalId":"912345678V","msisdn":"94771234567","idType":"NIC","idNumber":"912345678V"}'
```

(In local dev, `AuthServiceCustomerValidationClient` isn't wired up -- `NoOpCustomerValidationClient`
takes over and logs a loud warning; every customer validation succeeds. Real environments set
`AUTH_SERVICE_BASE_URL` and this must never happen -- see that class's javadoc.)

The MIFE Star Points core has no dev-reachable instance, so `MIFE_BASE_URL` points at a WireMock
container instead. Its stubs live in `deploy/local/wiremock/mappings/` -- one file per endpoint
`MifeLoyaltyCoreAdapter` calls, each returning the success status the adapter expects (balance
12500.00, registration returning PIN_SENT, transfers and donations accepted, plus canned EARN and
BURN history). Edit a mapping file and `docker compose restart wiremock-mife` to change a response;
to see exactly what the service sent, use `curl http://localhost:8089/__admin/requests`. The
`MIFE_SP_COUNTER_*` credentials in `docker-compose.yml` are fake and unchecked by the stub, but
must stay non-empty -- the adapter builds its request bodies with `Map.of`, which rejects nulls.

Building/testing without Docker:
```bash
mvn clean verify   # unit tests everywhere; loyalty-service also runs its Testcontainers-backed
                    # repository test, which needs a working Docker daemon
```

## Adding a new microservice (the productization pattern, Doc 1 sec 3 principle 1)

1. Copy `reports-service` (or `content-service` if Mongo fits better) as your starting layout:
   `pom.xml`, `Application.java`, `domain/`, `repository/`, `web/`, `application.yml`,
   `Dockerfile`, `logback-spring.xml`.
2. Add the module to the root `pom.xml`'s `<modules>` list.
3. Depend on `platform-common` -- tenant resolution, security, error handling, and observability
   are then already done.
4. If it calls an operator-specific core/third-party system, define one interface extending
   `ApiAdapter` (see `LoyaltyCoreAdapter`) plus a default adapter implementation, and register it
   via a `@PostConstruct` config class (see `LoyaltyAdapterConfig`).
5. Add a route in `api-gateway/src/main/resources/application.yml`, above the legacy-PHP
   fallback route.
6. Add a Helm values file under `deploy/helm/values/`, and a stage in the root `Jenkinsfile`
   (or just add the module name to the `SERVICES` build parameter -- the pipeline is generic).

No other module changes. This is deliberately the same five steps every time, which is the
whole point of the shared framework.

## Onboarding a new operator (Doc 1 sec 5)

1. Get access to the operator's Kubernetes cluster/VPN.
2. Copy `deploy/helm/values/operator-values/operator-acme-values.yaml` to a new
   `operator-<name>-values.yaml`, and fill in that operator's `TENANT_ID`, endpoints, and secret
   references.
3. Create that operator's `TenantConfig` document via `config-tenant-service`'s
   `PUT /api/v1/tenants/{tenantId}` (theme, host aliases, adapter bindings) and its layout
   documents via `PUT /api/v1/tenants/{tenantId}/layout/{screenKey}`.
4. `helm upgrade --install <service> deploy/helm/microservice-chart -f deploy/helm/values/<service>-values.yaml -f deploy/helm/values/operator-values/operator-<name>-values.yaml --namespace <name> --kube-context <name>-cluster`
   for every service that operator needs.
5. Nothing above touched application code. That's the test of whether this actually worked.

## Testing & CI (Doc 5 sec 6 checklist, applied)

- **Unit tests**: JUnit 5 + Mockito, see `LoyaltyServiceImplTest` for the pattern (mock the
  adapter registry/repository/event publisher, assert on business rules and on what got
  audited).
- **Integration tests**: Testcontainers spin up a real MySQL for `loyalty-service`'s repository
  layer (`LoyaltyTransactionAuditRepositoryIntegrationTest`) instead of mocking the database.
- **Coverage gate**: the root `pom.xml`'s `jacoco-maven-plugin` fails the build below 60% line
  coverage bundle-wide -- adjust the threshold as your suite matures.
- **CI pipeline**: see `Jenkinsfile` at the repo root and `ci/README.md` for what's wired vs.
  what's a TODO (QA/E2E automation against a running dev deploy, and performance testing, are
  both marked `TODO` there since they depend on suites that live in the frontend repos and on
  your k6 setup, not on this backend monorepo).

## Where this sits in the document package

This repo is the code behind **Doc 5's** 90-day pilot scope and **Doc 1 sec 4.3/6**'s target
architecture. Doc 2 is the SDLC toolchain this `Jenkinsfile` and Helm setup implement. Doc 4 is
the longer-term roadmap this pilot is the first wave of.
