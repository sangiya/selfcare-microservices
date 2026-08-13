// Shared env config, imported by playwright.config.js and specs. Kept as a plain module
// (not inside playwright.config.js) so specs can import just the values they need without
// pulling in Playwright's config loader.
module.exports = {
  WEB_BASE_URL: process.env.WEB_BASE_URL || 'http://localhost:3000',
  GATEWAY_URL: process.env.GATEWAY_URL || 'http://localhost:8080',
  TEST_TENANT_ID: process.env.TEST_TENANT_ID || 'acme-telecom',
};
