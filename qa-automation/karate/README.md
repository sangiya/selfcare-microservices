# Karate API Contracts

This suite complements the REST-Assured smoke tests with readable contract scenarios for the
gateway-facing API paths most likely to regress during routing, validation, and envelope
changes.

## Run

```
cd qa-automation/karate
mvn test
```

## Environment variables

- `GATEWAY_URL` default `http://localhost:8080`
- `LOYALTY_URL` default `http://localhost:8082`
- `TEST_TENANT_ID` default `acme-telecom`
