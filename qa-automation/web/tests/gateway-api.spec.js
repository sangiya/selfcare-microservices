// @ts-check
const { test, expect } = require('@playwright/test');
const { CONTENT_URL, TEST_TENANT_ID } = require('../qa.env');

/**
 * Playwright isn't just a browser driver -- its `request` fixture is a full HTTP client,
 * usable without launching a browser at all. These run against the content service TODAY,
 * before any React web app exists to click through, so `npx playwright test` gives you real
 * green (or red) results right now instead of an empty suite. They overlap a little with
 * ../../api/README.md's REST-Assured suite by design -- different language, same environment,
 * useful cross-check if one framework has a bug the other doesn't.
 */
test.describe('Content API (smoke)', () => {
  test('content articles endpoint returns a well-formed envelope', async ({ request }) => {
    const res = await request.get(`${CONTENT_URL}/api/v1/content/articles`, {
      params: { category: 'billing' },
      headers: { 'X-Tenant-Id': TEST_TENANT_ID },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
  });

  test('unknown content article returns a structured 404', async ({ request }) => {
    const res = await request.get(`${CONTENT_URL}/api/v1/content/articles/does-not-exist`, {
      headers: { 'X-Tenant-Id': TEST_TENANT_ID },
    });
    expect(res.status()).toBe(404);
    const body = await res.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('NOT_FOUND');
  });
});
