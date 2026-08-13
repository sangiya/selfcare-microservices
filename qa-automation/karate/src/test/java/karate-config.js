function fn() {
  var System = Java.type('java.lang.System');

  function env(name, fallback) {
    var value = System.getenv(name);
    return value && value.trim() ? value : fallback;
  }

  return {
    gatewayUrl: env('GATEWAY_URL', 'http://localhost:8080'),
    loyaltyUrl: env('LOYALTY_URL', 'http://localhost:8082'),
    testTenantId: env('TEST_TENANT_ID', 'acme-telecom')
  };
}
