package com.selfcare.apitests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The default provider adapter is intentionally simple, but the endpoint now runs through a real
 * delivery path and persists the resulting SENT/FAILED status instead of leaving requests queued.
 */
@Feature("Notification API")
class NotificationApiIT extends ApiTestBase {

    private static final String MSISDN = "94771234567";

    @Test
    @DisplayName("a direct send is dispatched and tagged as direct-api")
    void directSendIsDispatched() {
        Integer id = given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .queryParam("channel", "SMS")
                .queryParam("templateKey", "welcome-sms")
                .when()
                .post("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.tenantId", is(TENANT))
                .body("data.channel", is("SMS"))
                .body("data.templateKey", is("welcome-sms"))
                // Distinguishes an API-triggered notification from a Kafka event-triggered one.
                .body("data.sourceEvent", is("direct-api"))
                .body("data.status", is("SENT"))
                .body("data.id", notNullValue())
                .extract()
                .path("data.id");

        given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .when()
                .get("/api/v1/notifications")
                .then()
                .statusCode(200)
                .body("data.id", hasItem(id));
    }

    @Test
    @DisplayName("an unsupported channel is rejected rather than silently defaulted")
    void unsupportedChannelIsRejected() {
        given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .queryParam("channel", "CARRIER_PIGEON")
                .queryParam("templateKey", "welcome-sms")
                .when()
                .post("/api/v1/notifications")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }
}
