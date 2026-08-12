# notification-service -- porting TODO

The event-driven path is real and working: `LoyaltyEventListener` consumes
`loyalty.points.events` and `loyalty.partner-redemption.requested` (published by
`loyalty-service`'s `LoyaltyEventPublisher`) and persists a queued `NotificationRequest` --
run both services against the same Kafka broker (see `docker-compose.yml` at the repo root) to
see it end to end. What's still a TODO is everything below.

## Provider adapters (TODO)

Add a `NotificationProviderAdapter`-style interface per channel, following the exact pattern
`loyalty-service`'s `LoyaltyCoreAdapter` demonstrates (one interface, one default adapter
registered in the `ApiAdapterRegistry`, one operator-specific override if a given operator uses
a different SMS/push/email provider):

- `PushProviderAdapter` -- Firebase (the legacy `FirebaseController.php`, 9,222 LOC, is almost
  certainly where the real push-registration/send logic lives today -- read that one first).
- `SmsProviderAdapter` -- see `webapp/BlockSmsController.php` for the existing SMS-blocking
  logic that needs to be respected before any SMS actually sends.
- `EmailProviderAdapter` -- replaces the legacy `sendEmail()` calls.

Then change `LoyaltyEventListener` and `NotificationController` to actually call the adapter and
set status to `SENT`/`FAILED` instead of leaving every request `QUEUED`.

## Controllers to port, ranked by size (Doc 1 sec 2.3 audit)

| Module | Controller | LOC | Notes |
|---|---|---:|---|
| scapp | FirebaseController | 9,222 | Dominant controller in this domain -- push registration/send, likely also owns device-token management. |
| scapp | DashBoardNewController | 2,563 | Confirm overlap with config-tenant-service's layout documents before porting -- this may partially be a productization concern (Doc 1 sec 6.2), not a notification concern. |
| o2a | DashBoardNewController | 1,496 | |
| scapp | AppBannerController | 1,213 | |
| scapp | AdvertsController | 807 | |
| webapp | BlockSmsController | 747 | SMS opt-out/blocking -- port before wiring the SMS adapter live. |
| scapp | FbController | 610 | |
| scapp | ActionLinkingController | 464 | |
| scapp | SendConfirmSmsController | 211 | |
| scapp | FbAccController | 203 | |
| api | NotificationController | 145 | |
| scapp | DashboardCardController | 100 | |
| scapp | CallToActionController | 92 | |
| scapp | SystemAlertsController | 87 | |
| scapp | FbShareController | 86 | |
| scapp | NetNewNotificationController | 82 | |
| scapp | ChatController | 51 | |

**17 controllers, ~18,200 LOC total** (Doc 1 sec 4.3) -- Low risk tier, Doc 5 90-day pilot scope.
Several of the "DashBoard"/"Banner"/"Card" controllers above may actually belong in
config-tenant-service's layout model rather than here -- confirm with the team before porting,
per the note in that row.
