/**
 * PLACEHOLDER -- see ../README.md. Skipped by default (no DETOX_APP_READY env var) so
 * `detox test` doesn't fail red for an app that doesn't exist yet. Once the RN app exists and
 * .detoxrc.js's paths are updated:
 *
 *   1. Set DETOX_APP_READY=1 (or just delete the guard below) to enable this file.
 *   2. Replace the testID values with the RN app's real testID props
 *      (e.g. <TextInput testID="national-id-input" />).
 *   3. Add one file per real user flow, mirroring the flows already covered in
 *      ../../api's REST-Assured suite and ../../web's Playwright suite.
 */
const describeOrSkip = process.env.DETOX_APP_READY ? describe : describe.skip;

describeOrSkip('Loyalty balance flow (placeholder)', () => {
  beforeAll(async () => {
    await device.launchApp();
  });

  it('subscriber can view their loyalty balance', async () => {
    await element(by.id('national-id-input')).typeText('199012345678');
    await element(by.id('msisdn-input')).typeText('94771234567');
    await element(by.id('check-balance-button')).tap();
    await expect(element(by.id('points-balance'))).toBeVisible();
  });
});
