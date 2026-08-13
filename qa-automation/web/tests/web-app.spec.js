// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * PLACEHOLDER -- there is no React web/admin app in this repo yet (this monorepo is the Java
 * backend only). Skipped by default so `npx playwright test` doesn't fail red for something
 * that isn't built. Once the real frontend exists:
 *
 *   1. Point WEB_BASE_URL (see ../qa.env.js) at its dev server or deployed URL.
 *   2. Delete the test.skip(...) line below and fill in real selectors -- prefer
 *      getByRole/getByLabel/getByTestId over CSS selectors so tests survive styling changes.
 *   3. Add one spec file per real user flow (login, register for loyalty, view balance,
 *      transfer points, view content articles) mirroring the flows already covered by
 *      ../../api's REST-Assured suite, but through the actual UI this time.
 */
test.describe('Web app (placeholder — fill in once the React frontend exists)', () => {
  test.skip(true, 'No React web app in this repo yet -- see file header.');

  test('subscriber can view their loyalty balance', async ({ page }) => {
    await page.goto('/loyalty/balance');
    await page.getByLabel('National ID').fill('199012345678');
    await page.getByLabel('Mobile number').fill('94771234567');
    await page.getByRole('button', { name: 'Check balance' }).click();
    await expect(page.getByTestId('points-balance')).toBeVisible();
  });
});
