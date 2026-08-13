import { expect, test } from '@playwright/test';

/**
 * Browser-level coverage for the selfcare web/admin frontend, which lives outside this repo.
 * Skipped until WEB_BASE_URL points at a running frontend; the `web` project in
 * playwright.config.ts is already wired for it, so enabling these is a matter of setting that
 * variable and deleting the skip.
 */

test.describe('Selfcare web', () => {
  test.skip(
    !process.env.WEB_BASE_URL,
    'Set WEB_BASE_URL to the selfcare frontend to run browser coverage.',
  );

  test('a subscriber can see their points balance', async ({ page }) => {
    await page.goto('/loyalty');

    await expect(page.getByRole('heading', { name: /points/i })).toBeVisible();
    await expect(page.getByTestId('current-balance')).toBeVisible();
  });
});
