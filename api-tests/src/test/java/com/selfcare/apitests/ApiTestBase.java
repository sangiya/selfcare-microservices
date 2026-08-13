package com.selfcare.apitests;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Shared REST-Assured setup. Every request carries the tenant header the platform's
 * TenantContext filter expects, and is recorded into Allure via {@link AllureRestAssured} so a
 * failure report shows the exact request/response rather than just an assertion diff.
 *
 * <p>Target environment comes from {@code -Dapi.base.url}, defaulting to the local
 * docker-compose gateway.
 */
abstract class ApiTestBase {

    protected static final String TENANT = System.getProperty("tenant.id", "acme-telecom");
    protected static final String BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");

    protected static final RequestSpecification API = new RequestSpecBuilder()
            .setBaseUri(BASE_URL)
            .addHeader("X-Tenant-Id", TENANT)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .build();
}
