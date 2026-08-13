# Pact Consumer Contracts

This suite generates consumer-driven contracts for the gateway endpoints used by external
clients. It is intentionally self-contained and does not require the full microservice stack to
be running in order to publish pact files.

## Run

```
cd qa-automation/pact
mvn test
```

Generated pact files land in `target/pacts/`.
