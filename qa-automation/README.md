# QA Automation

Five suites, one per validation style, matching what the Jenkinsfile's `QA Automation (Dev)`
stage now runs:

| Surface | Tool | License | Status |
|---|---|---|---|
| Java backend smoke | REST-Assured | Apache 2.0 | Real, runs today against the docker-compose stack |
| API contracts | Karate | MIT | Real, runs today against the gateway and service endpoints |
| Consumer contracts | Pact | MIT | Real, runs today and generates pact files without needing the full stack |
| Web and admin | Playwright | Apache 2.0 | Gateway API checks run today; UI specs stay placeholders until the frontend exists |
| Mobile | Detox | MIT | Template only until the React Native app exists |

All five are free and open source.

## Run everything locally

```bash
# 1. Bring the stack up from the repo root
docker compose --profile app up -d --build

# 2. REST-Assured smoke suite
cd qa-automation/api && mvn test && cd ../..

# 3. Karate contract suite
cd qa-automation/karate && mvn test && cd ../..

# 4. Pact consumer contract suite
cd qa-automation/pact && mvn test && cd ../..

# 5. Playwright suite
cd qa-automation/web && npm install && npm run install-browsers && npm test && cd ../..

# 6. Detox
# Template only until the mobile app exists
```

## Results and dashboards

- REST-Assured writes Allure results to `api/target/allure-results/`
- Playwright writes Allure results to `web/allure-results/`
- Pact writes contract files to `pact/target/pacts/`
- Jenkins publishes JUnit, Allure, Playwright HTML, Checkov, ZAP, and Pact artifacts from the pipeline

To rebuild the combined Allure HTML report locally:

```bash
npm install -g allure-commandline
allure generate \
  qa-automation/api/target/allure-results \
  qa-automation/web/allure-results \
  -o qa-automation/allure-report --clean
allure open qa-automation/allure-report
```

## Honest gaps

- Web and mobile UI specs are still placeholders because this repo contains the Java backend, not the frontend applications.
- API auth is still using the local permit-all fallback until a real JWT issuer is wired into the environment.
- Pact Broker publication and provider-side verification still need broker credentials and environment policy outside this repo.
- The missing-request-parameter path still surfaces an existing `GlobalExceptionHandler` gap in `platform-common`.
