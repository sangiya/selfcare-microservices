// @ts-check
const { defineConfig, devices } = require('@playwright/test');
const { WEB_BASE_URL } = require('./qa.env');

module.exports = defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['allure-playwright', { resultsDir: 'allure-results' }],
  ],
  use: {
    baseURL: WEB_BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    // The repo contains backend-only gateway smoke checks plus a placeholder UI spec.
    // Until a real web frontend exists here, cross-browser duplication adds cost but no signal.
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
