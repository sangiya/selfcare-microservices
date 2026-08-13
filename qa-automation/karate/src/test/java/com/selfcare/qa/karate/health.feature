Feature: Service health contracts

  Scenario: API Gateway actuator health is UP
    Given url gatewayUrl
    And path 'actuator', 'health'
    When method get
    Then status 200
    And match response.status == 'UP'

  Scenario: Loyalty service actuator health is UP
    Given url loyaltyUrl
    And path 'actuator', 'health'
    When method get
    Then status 200
    And match response.status == 'UP'
