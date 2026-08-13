/** @type {import('jest').Config} */
module.exports = {
  rootDir: '..',
  testMatch: ['<rootDir>/e2e/**/*.test.js'],
  testTimeout: 120000,
  maxWorkers: 1,
  globalSetup: 'detox/runners/jest/globalSetup',
  globalTeardown: 'detox/runners/jest/globalTeardown',
  testEnvironment: 'detox/runners/jest/testEnvironment',
  // allure-jest keeps mobile results in the same format as the Playwright and REST-Assured
  // suites, so one `allure generate` covers all three.
  reporters: [
    'detox/runners/jest/reporter',
    ['allure-jest/node', { resultsDir: 'allure-results' }],
  ],
  verbose: true,
};
