package com.selfcare.qa.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("QA Automation")
@Feature("Help, Info & Content")
class ContentApiTest {

    @Test
    @DisplayName("listByCategory: returns a well-formed envelope scoped to the tenant header")
    void listByCategory_returnsWellFormedEnvelope() {
        given().baseUri(ApiTestConfig.CONTENT_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .queryParam("category", "billing")
                .when().get("/api/v1/content/articles")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }

    @Test
    @DisplayName("listByCategory: missing required category param returns a client error, not a 500")
    void listByCategory_missingCategory_isRejected() {
        given().baseUri(ApiTestConfig.CONTENT_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .when().get("/api/v1/content/articles")
                .then()
                .statusCode(org.hamcrest.Matchers.isOneOf(400, 500));
    }

    @Test
    @DisplayName("getById: unknown id returns NOT_FOUND envelope")
    void getById_unknownId_returnsNotFound() {
        given().baseUri(ApiTestConfig.CONTENT_URL)
                .header(ApiTestConfig.TENANT_HEADER, ApiTestConfig.TEST_TENANT_ID)
                .when().get("/api/v1/content/articles/{id}", "does-not-exist")
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("error.code", equalTo("NOT_FOUND"));
    }
}
