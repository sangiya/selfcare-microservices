Feature: Loyalty service API contracts

  Scenario: Valid register request reaches the business flow
    Given url loyaltyUrl
    And path 'api', 'v1', 'loyalty', 'register'
    And header X-Tenant-Id = testTenantId
    And request
      """
      {
        "nationalId": "199012345678",
        "msisdn": "94771234567",
        "idType": "NIC",
        "idNumber": "199012345678",
        "email": "karate@example.com"
      }
      """
    When method post
    * match [200, 502] contains responseStatus
    * match response.success == '#boolean'

  Scenario: Invalid register request returns a validation error envelope
    Given url loyaltyUrl
    And path 'api', 'v1', 'loyalty', 'register'
    And header X-Tenant-Id = testTenantId
    And request
      """
      {
        "msisdn": "94771234567"
      }
      """
    When method post
    Then status 400
    And match response.success == false
    And match response.error.code == 'VALIDATION_ERROR'
