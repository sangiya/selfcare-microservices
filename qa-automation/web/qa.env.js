function deriveServiceUrl(baseUrl, port, fallback) {
  try {
    const url = new URL(baseUrl);
    url.port = String(port);
    url.pathname = '';
    url.search = '';
    url.hash = '';
    return url.toString().replace(/\/$/, '');
  } catch {
    return fallback;
  }
}

const gatewayUrl = process.env.GATEWAY_URL || 'http://localhost:8080';

// Shared env config, imported by playwright.config.js and specs. Kept as a plain module
// (not inside playwright.config.js) so specs can import just the values they need without
// pulling in Playwright's config loader.
module.exports = {
  WEB_BASE_URL: process.env.WEB_BASE_URL || 'http://localhost:3000',
  GATEWAY_URL: gatewayUrl,
  CONTENT_URL: process.env.CONTENT_URL || deriveServiceUrl(gatewayUrl, 8085, 'http://localhost:8085'),
  TEST_TENANT_ID: process.env.TEST_TENANT_ID || 'acme-telecom',
};
