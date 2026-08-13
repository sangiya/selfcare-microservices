import { defineConfig, devices } from '@playwright/test';

// Everything is overridable by env var so the same suite runs against local docker-compose and
// against the dev environment the pipeline deploys to (Jenkinsfile "QA Automation (Dev)").
const apiBaseURL = process.env.API_BASE_URL ?? 'http://localhost:8080';
const webBaseURL = process.env.WEB_BASE_URL ?? 'http://localhost:3000';
const tenantId = process.env.TENANT_ID ?? 'acme-telecom';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // the loyalty specs assert on a shared audit trail; keep them ordered
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // junit is always emitted so the Jenkins stage can publish results without extra flags;
  // allure-results uses the same format as the REST-Assured and Detox suites so one
  // `allure generate` covers web, API and mobile together.
  reporter: [
    ['list'],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['html', { open: 'never' }],
    ['allure-playwright', { resultsDir: 'allure-results' }],
  ],
  use: {
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      // No browser is launched for these -- they drive the platform through the API gateway
      // using Playwright's request fixture.
      name: 'api',
      testDir: './tests/api',
      use: {
        baseURL: apiBaseURL,
        extraHTTPHeaders: {
          'X-Tenant-Id': tenantId,
          Accept: 'application/json',
        },
      },
    },
    {
      // Inert until WEB_BASE_URL points at a real selfcare frontend -- see tests/ui.
      name: 'web',
      testDir: './tests/ui',
      use: {
        ...devices['Desktop Chrome'],
        baseURL: webBaseURL,
      },
    },
  ],
});
