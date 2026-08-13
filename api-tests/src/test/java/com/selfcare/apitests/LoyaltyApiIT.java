package com.selfcare.apitests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract-level checks for the loyalty API as a client sees it. Deliberately complements the
 * Playwright suite rather than duplicating it: Playwright asserts multi-step journeys across
 * services, these assert the HTTP contract of each endpoint -- status codes, the shared
 * envelope, error codes and validation behaviour.
 */
@Feature("Loyalty API")
class LoyaltyApiIT extends ApiTestBase {

    private static final String NATIONAL_ID = "912345678V";
    private static final String MSISDN = "94771234567";

    @Test
    @DisplayName("balance responds with the standard success envelope")
    @Description("Every service shares one response envelope; a client should never have to "
            + "special-case a per-service shape.")
    void balanceReturnsStandardEnvelope() {
        given().spec(API)
                .queryParam("nationalId", NATIONAL_ID)
                .queryParam("msisdn", MSISDN)
                .when()
                .get("/api/v1/loyalty/balance")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", is(true))
                .body("timestamp", notNullValue())
                .body("error", nullOrAbsent())
                .body("data.currentBalance", greaterThanOrEqualTo(0f))
                .body("data.redeemableBalance", greaterThanOrEqualTo(0f));
    }

    @Test
    @DisplayName("register returns a known status and a transaction reference")
    void registerReturnsRecognisedStatus() {
        given().spec(API)
                .contentType(ContentType.JSON)
                .body("""
                        {"nationalId":"%s","msisdn":"%s","idType":"NIC","idNumber":"%s"}
                        """.formatted(NATIONAL_ID, MSISDN, NATIONAL_ID))
                .when()
                .post("/api/v1/loyalty/register")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.status", is(oneOf("REGISTERED", "PIN_SENT")))
                .body("data.pinTransactionRef", not(equalTo(null)));
    }

    @Test
    @DisplayName("history entries are all classified as EARN or BURN")
    void historyEntriesAreClassified() {
        given().spec(API)
                .queryParam("nationalId", NATIONAL_ID)
                .queryParam("msisdn", MSISDN)
                .queryParam("listSize", 10)
                .when()
                .get("/api/v1/loyalty/history")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data.transactionType.unique()", everyItemIsEarnOrBurn());
    }

    @Test
    @DisplayName("an incomplete register body is rejected with VALIDATION_ERROR")
    void invalidRegisterBodyIsRejected() {
        given().spec(API)
                .contentType(ContentType.JSON)
                .body("{\"nationalId\":\"\"}")
                .when()
                .post("/api/v1/loyalty/register")
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.code", is("VALIDATION_ERROR"))
                .body("error.traceId", notNullValue())
                .body("error.message", allOf(containsString("required"), containsString("msisdn")));
    }

    @Test
    @DisplayName("a missing required query parameter is a client error, not a server error")
    @Description("Regression: these used to fall through to the catch-all handler and come back "
            + "as 500 INTERNAL_ERROR, blaming the service for a caller mistake.")
    void missingRequiredParameterIsRejected() {
        given().spec(API)
                .queryParam("msisdn", MSISDN) // nationalId deliberately omitted
                .when()
                .get("/api/v1/loyalty/balance")
                .then()
                .statusCode(400)
                .body("error.code", is("VALIDATION_ERROR"))
                .body("error.message", containsString("nationalId"));
    }

    @Test
    @DisplayName("a non-positive transfer amount is rejected")
    void nonPositiveTransferAmountIsRejected() {
        given().spec(API)
                .contentType(ContentType.JSON)
                .body("""
                        {"nationalId":"%s","fromMsisdn":"%s","channel":"MOBILE",
                         "toIdentifier":"94779999999","amount":0}
                        """.formatted(NATIONAL_ID, MSISDN))
                .when()
                .post("/api/v1/loyalty/transfer")
                .then()
                .statusCode(400)
                .body("error.code", is("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("an unknown transfer channel is rejected rather than defaulted")
    void unknownTransferChannelIsRejected() {
        given().spec(API)
                .contentType(ContentType.JSON)
                .body("""
                        {"nationalId":"%s","fromMsisdn":"%s","channel":"CARRIER_PIGEON",
                         "toIdentifier":"94779999999","amount":10}
                        """.formatted(NATIONAL_ID, MSISDN))
                .when()
                .post("/api/v1/loyalty/transfer")
                .then()
                .statusCode(400)
                .body("success", is(false));
    }

    private static org.hamcrest.Matcher<Object> nullOrAbsent() {
        return org.hamcrest.Matchers.nullValue();
    }

    private static org.hamcrest.Matcher<Iterable<? extends String>> everyItemIsEarnOrBurn() {
        return org.hamcrest.Matchers.everyItem(is(oneOf("EARN", "BURN")));
    }
}
