package com.selfcare.qa.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cheapest-possible smoke check: is every container actually up and did Spring context start
 * cleanly. Run this first -- if any of these fail, nothing else in this suite is worth running,
 * and it tells you which service to go check `docker compose logs` on.
 */
@Epic("QA Automation")
@Feature("Service health")
class ServiceHealthTest {

    @Test
    @DisplayName("API Gateway actuator health is UP")
    @Description("Gateway's own health, independent of any downstream service.")
    void gatewayHealthIsUp() {
        given().baseUri(ApiTestConfig.GATEWAY_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Loyalty service actuator health is UP")
    void loyaltyHealthIsUp() {
        given().baseUri(ApiTestConfig.LOYALTY_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Reports service actuator health is UP")
    void reportsHealthIsUp() {
        given().baseUri(ApiTestConfig.REPORTS_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Notification service actuator health is UP")
    void notificationHealthIsUp() {
        given().baseUri(ApiTestConfig.NOTIFICATION_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Content service actuator health is UP")
    void contentHealthIsUp() {
        given().baseUri(ApiTestConfig.CONTENT_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Config/tenant service actuator health is UP")
    void configTenantHealthIsUp() {
        given().baseUri(ApiTestConfig.CONFIG_TENANT_URL)
                .when().get("/actuator/health")
                .then().statusCode(200)
                .body("status", equalTo("UP"));
    }
}
