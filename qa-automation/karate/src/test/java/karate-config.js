function fn() {
  var System = Java.type('java.lang.System');
  var URI = Java.type('java.net.URI');

  function env(name, fallback) {
    var value = System.getenv(name);
    return value && value.trim() ? value : fallback;
  }

  function deriveServiceUrl(baseUrl, port, fallback) {
    try {
      var uri = new URI(baseUrl);
      if (uri.getScheme() && uri.getHost()) {
        return uri.getScheme() + '://' + uri.getHost() + ':' + port;
      }
    } catch (e) {
      // Ignore malformed base URLs and fall back to the suite's local default.
    }
    return fallback;
  }

  var gatewayUrl = env('GATEWAY_URL', 'http://localhost:8080');

  return {
    gatewayUrl: gatewayUrl,
    loyaltyUrl: env('LOYALTY_URL', deriveServiceUrl(gatewayUrl, 8082, 'http://localhost:8082')),
    testTenantId: env('TEST_TENANT_ID', 'acme-telecom')
  };
}
