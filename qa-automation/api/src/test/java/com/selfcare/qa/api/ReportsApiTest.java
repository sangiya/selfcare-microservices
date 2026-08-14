package com.selfcare.qa.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Covers the "submit -> check status -> list" shell that's actually implemented today (see
 * ReportsController's javadoc: per-ReportType generation is still a TODO). Extend this file
 * alongside that generation work rather than guessing at behavior that doesn't exist yet --
 * same rule the reports-service unit tests already follow.
 */
@Epic("QA Automation")
@Feature("Reports & CDR")
class ReportsApiTest {

    @Test
    @DisplayName("submit -> get status: a freshly submitted report is PENDING and fetchable by id")
    void submitThenGetStatus_roundTrips() {
        String msisdn = "94779990001";

        Integer id = given().baseUri(ApiTestConfig.REPORTS_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .queryParam("subscriberMsisdn", msisdn)
                .queryParam("reportType", "ACTIVITY_REPORT")
                .queryParam("fromDate", "2026-01-01")
                .queryParam("toDate", "2026-01-31")
                .when().post("/api/v1/reports/requests")
                .then()
                .statusCode(202)
                .body("success", equalTo(true))
                .body("data.status", equalTo("PENDING"))
                .extract().path("data.id");

        given().baseUri(ApiTestConfig.REPORTS_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .when().get("/api/v1/reports/requests/{id}", id)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("getStatus: unknown id returns NOT_FOUND envelope")
    void getStatus_unknownId_returnsNotFound() {
        given().baseUri(ApiTestConfig.REPORTS_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .when().get("/api/v1/reports/requests/{id}", 999_999_999L)
                .then()
                .statusCode(404)
                .body("error.code", equalTo("NOT_FOUND"));
    }
}
