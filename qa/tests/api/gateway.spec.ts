import { expect, test } from '@playwright/test';

/**
 * The gateway is the only entry point (Doc 1 sec 4), so these check that it is healthy and that
 * a migrated domain is actually routed to its service rather than falling through to the
 * strangler-fig legacy backend.
 */

test.describe('API gateway', () => {
  test('reports itself healthy', async ({ request }) => {
    const response = await request.get('/actuator/health');

    expect(response.status()).toBe(200);
    expect((await response.json()).status).toBe('UP');
  });

  test('routes a migrated domain to its service instead of the legacy fallback', async ({ request }) => {
    // A migrated route answers with the platform's own envelope; the legacy fallback route
    // would either fail to connect or return something that isn't this shape.
    const response = await request.get('/api/v1/loyalty/balance', {
      params: { nationalId: '912345678V', msisdn: '94771234567' },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty('success');
    expect(body).toHaveProperty('timestamp');
  });
});
