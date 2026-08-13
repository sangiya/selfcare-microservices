# QA Automation — API (REST-Assured)

Black-box tests that hit a **running** stack over real HTTP (docker-compose locally, or a
deployed dev namespace in CI). They are not part of the root Maven reactor on purpose — see the
comment at the top of `pom.xml`.

## Run locally

1. Start the app stack first (these tests need it running):
   ```
   docker compose --profile app up -d --build
   ```
2. From this directory:
   ```
   cd qa-automation/api
   mvn test
   ```
   That's it — no env vars needed if you're running the default docker-compose port mapping
   (gateway `8080`, loyalty `8082`, reports `8083`, notification `8084`, content `8085`,
   config-tenant `8081`).

## Check the results

- Console output shows pass/fail per test as Maven runs.
- Machine-readable results: `target/surefire-reports/*.xml` (same format `junit` step in
  Jenkins already reads for the unit-test stage).
- Allure results: `target/allure-results/` — see `../README.md` for generating the combined
  HTML report across all three QA suites (API + web + mobile).

## Pointing at a different environment

Every base URL is overridable via env var (see `ApiTestConfig.java`):

```
GATEWAY_URL=https://dev.example.com \
LOYALTY_URL=https://dev.example.com/loyalty \
mvn test
```

If a service isn't reachable directly outside its Docker network (only the gateway is
publicly routable in a real deployment), point `LOYALTY_URL` etc. at whatever internal address
CI's `docker network` or Kubernetes service DNS resolves — Jenkinsfile's QA stage sets these
from the same values used in the `helm upgrade` step just before it.

## What's covered vs. not

- **Covered**: gateway routing, tenant-header propagation (`X-Tenant-Id`), request validation
  error shape, structured error envelopes, the reports submit/status/list shell, content
  articles lookup, loyalty register/balance against the (currently unwired) core adapter.
- **Not covered yet**: anything behind a real JWT (all services run in the dev permit-all
  fallback locally — see `ResourceServerSecurityConfig`). Once `AUTH_SERVICE_JWK_URI` is set to
  a real issuer, these tests will need a token-acquisition step added before each `given()`.
- **Known gap this suite surfaces, not fixes**: `GlobalExceptionHandler` has no handler for
  `MissingServletRequestParameterException`, so a missing required query param currently falls
  through to the generic 500 handler instead of a 400. `LoyaltyApiTest`/`ContentApiTest`
  document this rather than silently passing either way — worth a real fix in
  `platform-common` when you have a moment.
