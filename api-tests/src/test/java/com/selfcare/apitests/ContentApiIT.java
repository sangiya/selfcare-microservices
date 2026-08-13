package com.selfcare.apitests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Contract checks for the content catalogue. */
@Feature("Content API")
class ContentApiIT extends ApiTestBase {

    @Test
    @DisplayName("listing a category returns the standard envelope even when empty")
    void listByCategoryReturnsEnvelope() {
        given().spec(API)
                .queryParam("category", "billing")
                .when()
                .get("/api/v1/content/articles")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("data", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("an unknown article id is a 404 with the shared error envelope")
    void unknownArticleIsNotFound() {
        given().spec(API)
                .when()
                .get("/api/v1/content/articles/{id}", "does-not-exist")
                .then()
                .statusCode(404)
                .body("success", is(false))
                .body("error.code", is("NOT_FOUND"))
                .body("error.traceId", notNullValue());
    }
}
