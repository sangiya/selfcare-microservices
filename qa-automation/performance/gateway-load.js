import http from 'k6/http';
import { check, group, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const tenantId = __ENV.TENANT_ID || 'acme-telecom';
const commonHeaders = {
  'X-Tenant-ID': tenantId,
};

export const options = {
  scenarios: {
    health: {
      executor: 'constant-arrival-rate',
      exec: 'healthScenario',
      rate: 12,
      timeUnit: '1s',
      duration: '45s',
      preAllocatedVUs: 8,
      maxVUs: 20,
    },
    catalogue: {
      executor: 'ramping-arrival-rate',
      exec: 'catalogueScenario',
      startRate: 2,
      timeUnit: '1s',
      preAllocatedVUs: 6,
      maxVUs: 18,
      stages: [
        { duration: '20s', target: 4 },
        { duration: '20s', target: 8 },
        { duration: '20s', target: 4 },
      ],
    },
    loyaltyReads: {
      executor: 'ramping-arrival-rate',
      exec: 'loyaltyScenario',
      startRate: 2,
      timeUnit: '1s',
      preAllocatedVUs: 6,
      maxVUs: 18,
      stages: [
        { duration: '20s', target: 3 },
        { duration: '20s', target: 6 },
        { duration: '20s', target: 3 },
      ],
    },
    reportsFlow: {
      executor: 'constant-arrival-rate',
      exec: 'reportsScenario',
      rate: 2,
      timeUnit: '1s',
      duration: '45s',
      preAllocatedVUs: 4,
      maxVUs: 10,
    },
    notificationsRead: {
      executor: 'constant-arrival-rate',
      exec: 'notificationsScenario',
      rate: 4,
      timeUnit: '1s',
      duration: '45s',
      preAllocatedVUs: 4,
      maxVUs: 10,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    'http_req_duration{journey:health}': ['p(95)<500'],
    'http_req_duration{journey:catalogue}': ['p(95)<1200'],
    'http_req_duration{journey:loyalty-balance}': ['p(95)<1200'],
    'http_req_duration{journey:reports-submit}': ['p(95)<1200'],
    'http_req_duration{journey:reports-get}': ['p(95)<1200'],
    'http_req_duration{journey:notifications-list}': ['p(95)<1200'],
  },
};

function request(method, path, params = {}) {
  const merged = {
    headers: commonHeaders,
    tags: params.tags || {},
  };
  if (params.headers) {
    merged.headers = { ...commonHeaders, ...params.headers };
  }
  return http.request(method, `${baseUrl}${path}`, params.body, merged);
}

export function healthScenario() {
  group('gateway-health', () => {
    const response = request('GET', '/actuator/health', {
      tags: { journey: 'health' },
    });
    check(response, {
      'health status is 200': (r) => r.status === 200,
      'health body reports UP': (r) => r.body.includes('"UP"'),
    });
  });
  sleep(1);
}

export function catalogueScenario() {
  group('content-catalogue', () => {
    const response = request('GET', '/api/v1/content/articles?category=billing', {
      tags: { journey: 'catalogue' },
    });
    check(response, {
      'catalogue status is 200': (r) => r.status === 200,
      'catalogue envelope success=true': (r) => r.body.includes('"success":true'),
    });
  });
  sleep(1);
}

export function loyaltyScenario() {
  group('loyalty-balance', () => {
    const response = request(
      'GET',
      '/api/v1/loyalty/balance?nationalId=912345678V&msisdn=94771234567',
      {
        tags: { journey: 'loyalty-balance' },
      },
    );
    check(response, {
      'loyalty balance status is 200': (r) => r.status === 200,
      'loyalty balance envelope success=true': (r) => r.body.includes('"success":true'),
    });
  });
  sleep(1);
}

export function reportsScenario() {
  group('reports-flow', () => {
    const submitResponse = request(
      'POST',
      '/api/v1/reports/requests?subscriberMsisdn=94779990001&reportType=ACTIVITY_REPORT&fromDate=2026-01-01&toDate=2026-01-31',
      {
        tags: { journey: 'reports-submit' },
      },
    );
    const created =
      submitResponse.status === 202 ? JSON.parse(submitResponse.body).data?.id : null;

    check(submitResponse, {
      'report submit status is 202': (r) => r.status === 202,
      'report submit returns id': () => created !== null && created !== undefined,
    });

    if (created) {
      const getResponse = request('GET', `/api/v1/reports/requests/${created}`, {
        tags: { journey: 'reports-get' },
      });
      check(getResponse, {
        'report get status is 200': (r) => r.status === 200,
        'report get envelope success=true': (r) => r.body.includes('"success":true'),
      });
    }
  });
  sleep(1);
}

export function notificationsScenario() {
  group('notifications-list', () => {
    const response = request(
      'GET',
      '/api/v1/notifications?subscriberMsisdn=94771234567',
      {
        tags: { journey: 'notifications-list' },
      },
    );
    check(response, {
      'notifications list status is 200': (r) => r.status === 200,
      'notifications list envelope success=true': (r) => r.body.includes('"success":true'),
    });
  });
  sleep(1);
}
