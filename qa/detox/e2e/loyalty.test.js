/* global device, element, by, expect */

/**
 * Mirrors the loyalty journeys the API and Playwright suites already cover, but through the
 * React Native UI. Selectors use testID rather than text so they survive copy changes and
 * localisation -- the mobile app needs to set those testIDs for these to bind.
 *
 * These are the flows worth covering first; they will fail until the app exists, which is why
 * the pipeline stage that runs them is gated on RN_APP_ROOT being set.
 */
describe('Loyalty points (mobile)', () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: true });
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  it('shows the current points balance on the loyalty screen', async () => {
    await element(by.id('nav-loyalty')).tap();

    await expect(element(by.id('loyalty-screen'))).toBeVisible();
    await expect(element(by.id('current-balance'))).toBeVisible();
    await expect(element(by.id('redeemable-balance'))).toBeVisible();
  });

  it('lists earn and burn history', async () => {
    await element(by.id('nav-loyalty')).tap();
    await element(by.id('tab-history')).tap();

    await expect(element(by.id('history-list'))).toBeVisible();
  });

  it('rejects a transfer for more points than are redeemable', async () => {
    await element(by.id('nav-loyalty')).tap();
    await element(by.id('action-transfer')).tap();

    await element(by.id('transfer-recipient')).typeText('94779999999');
    await element(by.id('transfer-amount')).typeText('99999999');
    await element(by.id('transfer-submit')).tap();

    await expect(element(by.id('transfer-error'))).toBeVisible();
  });
});
