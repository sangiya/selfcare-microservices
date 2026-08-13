import { expect, test } from '@playwright/test';

const NOTIFICATIONS = '/api/v1/notifications';
const MSISDN = '94771234567';

test.describe('Notifications API', () => {
  test('direct send dispatches immediately and can be listed for the subscriber', async ({ request }) => {
    const send = await request.post(NOTIFICATIONS, {
      params: {
        subscriberMsisdn: MSISDN,
        channel: 'SMS',
        templateKey: 'welcome-sms',
      },
    });

    expect(send.status()).toBe(200);
    const sent = await send.json();
    expect(sent.success).toBe(true);
    expect(sent.data.subscriberMsisdn).toBe(MSISDN);
    expect(sent.data.channel).toBe('SMS');
    expect(sent.data.templateKey).toBe('welcome-sms');
    expect(sent.data.sourceEvent).toBe('direct-api');
    expect(sent.data.status).toBe('SENT');

    const list = await request.get(NOTIFICATIONS, {
      params: { subscriberMsisdn: MSISDN },
    });

    expect(list.status()).toBe(200);
    const body = await list.json();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
    expect(body.data.some((item: { id: number }) => item.id === sent.data.id)).toBe(true);
  });

  test('unsupported channels are rejected with the platform error envelope', async ({ request }) => {
    const response = await request.post(NOTIFICATIONS, {
      params: {
        subscriberMsisdn: MSISDN,
        channel: 'CARRIER_PIGEON',
        templateKey: 'welcome-sms',
      },
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body.error).toBeTruthy();
    expect(body.error.traceId).toBeTruthy();
  });
});
