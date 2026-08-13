import { expect, test } from '@playwright/test';

/**
 * Drives the loyalty domain end to end through the API gateway: gateway routing -> loyalty
 * service -> loyalty core adapter (MIFE, stubbed by WireMock in local dev) -> MySQL audit trail.
 *
 * Assertions are on contract shape and invariants rather than the WireMock stub's exact numbers,
 * so the same specs pass against a dev environment wired to a real loyalty core.
 */

const NATIONAL_ID = '912345678V';
const MSISDN = '94771234567';
const LOYALTY = '/api/v1/loyalty';

test.describe('Loyalty API', () => {
  test('balance returns a redeemable amount that never exceeds the current balance', async ({ request }) => {
    const response = await request.get(`${LOYALTY}/balance`, {
      params: { nationalId: NATIONAL_ID, msisdn: MSISDN },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(typeof body.data.currentBalance).toBe('number');
    expect(typeof body.data.redeemableBalance).toBe('number');
    expect(body.data.currentBalance).toBeGreaterThanOrEqual(0);
    expect(body.data.redeemableBalance).toBeLessThanOrEqual(body.data.currentBalance);
  });

  test('register returns a recognised status and a transaction reference', async ({ request }) => {
    const response = await request.post(`${LOYALTY}/register`, {
      data: {
        nationalId: NATIONAL_ID,
        msisdn: MSISDN,
        idType: 'NIC',
        idNumber: NATIONAL_ID,
      },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(['REGISTERED', 'PIN_SENT']).toContain(body.data.status);
    expect(body.data.pinTransactionRef).toBeTruthy();
  });

  test('history merges earn and burn transactions from the loyalty core', async ({ request }) => {
    const response = await request.get(`${LOYALTY}/history`, {
      params: { nationalId: NATIONAL_ID, msisdn: MSISDN, listSize: 10 },
    });

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);

    for (const entry of body.data) {
      expect(['EARN', 'BURN']).toContain(entry.transactionType);
      expect(entry.transactionSerial).toBeTruthy();
      expect(typeof entry.amount).toBe('number');
    }
  });

  test('a transfer is accepted and lands in the service audit trail', async ({ request }) => {
    // Unique per run so the assertion can't pass on a previous run's row.
    const counterparty = `9477${Date.now().toString().slice(-7)}`;

    const transfer = await request.post(`${LOYALTY}/transfer`, {
      data: {
        nationalId: NATIONAL_ID,
        fromMsisdn: MSISDN,
        channel: 'MOBILE',
        toIdentifier: counterparty,
        amount: 25.0,
      },
    });
    expect(transfer.status()).toBe(200);
    expect((await transfer.json()).success).toBe(true);

    const activity = await request.get(`${LOYALTY}/activity`, {
      params: { msisdn: MSISDN, page: 0, size: 20 },
    });
    expect(activity.status()).toBe(200);

    const items = (await activity.json()).data;
    const recorded = items.find(
      (item: { actionType: string; counterparty: string }) =>
        item.actionType === 'TRANSFER' && item.counterparty === counterparty,
    );

    expect(recorded, 'transfer should be persisted to the audit trail').toBeTruthy();
    expect(recorded.status).toBe('SUCCESS');
    expect(recorded.amount).toBe(25.0);
  });

  test('a donation is accepted', async ({ request }) => {
    const response = await request.post(`${LOYALTY}/donate`, {
      data: {
        nationalId: NATIONAL_ID,
        msisdn: MSISDN,
        donationAlias: 'charity-alias',
        amount: 10.0,
      },
    });

    expect(response.status()).toBe(200);
    expect((await response.json()).success).toBe(true);
  });

  test('an invalid request body is rejected with the standard error envelope', async ({ request }) => {
    const response = await request.post(`${LOYALTY}/register`, {
      data: { nationalId: '' },
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.error.code).toBe('VALIDATION_ERROR');
    expect(body.error.message).toContain('required');
    expect(body.error.traceId).toBeTruthy();
  });

  test('a transfer below the minimum amount is rejected', async ({ request }) => {
    const response = await request.post(`${LOYALTY}/transfer`, {
      data: {
        nationalId: NATIONAL_ID,
        fromMsisdn: MSISDN,
        channel: 'MOBILE',
        toIdentifier: '94779999999',
        amount: 0,
      },
    });

    expect(response.status()).toBe(400);
    expect((await response.json()).error.code).toBe('VALIDATION_ERROR');
  });
});
