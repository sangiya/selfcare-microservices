package com.selfcare.apitests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contract checks for the reports request lifecycle that is actually implemented today. */
@Feature("Reports API")
class ReportsApiIT extends ApiTestBase {

    private static final String MSISDN = "94771234567";

    @Test
    @DisplayName("submitting a report is accepted as 202 and starts life PENDING")
    void submitReportIsAcceptedAsPending() {
        given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .queryParam("reportType", "ACTIVITY_REPORT")
                .queryParam("fromDate", "2026-01-01")
                .queryParam("toDate", "2026-01-31")
                .when()
                .post("/api/v1/reports/requests")
                .then()
                // 202, not 200: generation is asynchronous and hasn't happened yet.
                .statusCode(202)
                .body("success", is(true))
                .body("data.id", notNullValue())
                .body("data.tenantId", is(TENANT))
                .body("data.subscriberMsisdn", is(MSISDN))
                .body("data.reportType", is("ACTIVITY_REPORT"))
                .body("data.status", is("PENDING"));
    }

    @Test
    @DisplayName("a submitted report can be read back by id and appears in the subscriber list")
    void submittedReportIsRetrievable() {
        Integer id = given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .queryParam("reportType", "ACTIVITY_REPORT")
                .when()
                .post("/api/v1/reports/requests")
                .then()
                .statusCode(202)
                .extract()
                .path("data.id");

        given().spec(API)
                .when()
                .get("/api/v1/reports/requests/{id}", id)
                .then()
                .statusCode(200)
                .body("data.id", is(id));

        given().spec(API)
                .queryParam("subscriberMsisdn", MSISDN)
                .when()
                .get("/api/v1/reports/requests")
                .then()
                .statusCode(200)
                .body("data.id", hasItem(id));
    }

    @Test
    @DisplayName("an unknown report id is a 404 with the shared error envelope")
    void unknownReportIdIsNotFound() {
        given().spec(API)
                .when()
                .get("/api/v1/reports/requests/{id}", 99_999_999)
                .then()
                .statusCode(404)
                .body("success", is(false))
                .body("error.code", is("NOT_FOUND"))
                .body("error.traceId", notNullValue());
    }
}
