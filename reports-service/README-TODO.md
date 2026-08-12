# reports-service -- porting TODO

This module is a **starter**: it has the same layering as `loyalty-service` (entity → repository
→ controller, wired to `platform-common`, Flyway-managed schema, tenant/audit/observability all
already working) but the actual report-generation business logic from the legacy PHP controllers
has not been ported. Do that domain's actual business logic here, following the pattern
`loyalty-service` demonstrates (one `LoyaltyCoreAdapter`-style adapter interface per external
system it calls, one audit/event record per action, full test coverage before it takes traffic --
see Doc 5 sec 6 for the non-negotiable checklist).

## Controllers to port, ranked by size (Doc 1 sec 2.3 audit)

| Module | Controller | LOC | Notes |
|---|---|---:|---|
| scapp | ActivityReportsController | 3,668 | Largest in this domain -- read this one first to size the real effort. |
| scapp | ChargingHistoryController | 2,531 | |
| scapp | CdrController | 1,459 | |
| scapp | FbCdrController | 545 | |
| cdr | PeriodicReportController | 464 | |
| scapp | ConnectionUsageController | 446 | |
| webapp | UsageHistoryController | 379 | |
| scapp | SendDrgReportController | 337 | |
| scapp | SendSimPurchaseReportController | 325 | |
| cdr | MmsProvisioningReportController | 321 | |
| scapp | SendReportsController | 289 | |
| cdr | SmsDeliveryReportController | 277 | |
| cdr | MmsDeliveryReportController | 274 | |
| cdr | MmsScheduleReportController | 272 | |
| cdr | CdrSearchController | 225 | |
| scapp | StatistcReportsController | 213 | |
| scapp | ClickHouseController | 96 | Confirm whether reports should query ClickHouse directly or through a new read API -- this controller suggests ClickHouse is already a report data source. |
| o2a | ReportsController | 70 | |
| cdr | DefaultController | 9 | |

**19 controllers, ~12,200 LOC total** (Doc 1 sec 4.3) -- Low risk tier, part of the Doc 5 90-day
pilot scope.

## Suggested approach

1. Read `ActivityReportsController.php` and `ChargingHistoryController.php` first (85% of this
   domain's LOC) to confirm whether reports are generated synchronously or as background jobs,
   and which data store they actually query (CDR store? ClickHouse? MySQL?).
2. Extend `ReportType` and the `ReportRequest` entity/table as needed once the real data sources
   are confirmed -- the current shape assumes an async "submit → poll" pattern, which may not fit
   every controller here.
3. Add a `ReportGenerator` interface with one implementation per `ReportType` (mirrors
   `LoyaltyCoreAdapter` in loyalty-service), invoked from a Kafka-consumer-driven background
   worker rather than synchronously in the controller thread.
4. Bring test coverage, security scanning, and performance testing up to the same bar as
   loyalty-service before this takes real traffic (Doc 5 sec 6).
