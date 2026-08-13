package com.selfcare.qa.pact;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "selfcare-api-gateway")
class NotificationGatewayConsumerPactTest {

    @Pact(provider = "selfcare-api-gateway", consumer = "selfcare-web-app")
    V4Pact directNotificationSendContract(PactDslWithProvider builder) {
        DslPart responseBody = new PactDslJsonBody()
                .booleanType("success", true)
                .object("data")
                .stringValue("tenantId", "acme-telecom")
                .stringValue("channel", "SMS")
                .stringValue("templateKey", "welcome-sms")
                .stringValue("sourceEvent", "direct-api")
                .stringValue("status", "SENT")
                .integerType("id", 1)
                .closeObject();

        return builder
                .given("notification delivery succeeds")
                .uponReceiving("a direct notification send request")
                .path("/api/v1/notifications")
                .query("subscriberMsisdn=94771234567&channel=SMS&templateKey=welcome-sms")
                .method("POST")
                .headers("X-Tenant-Id", "acme-telecom")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "directNotificationSendContract")
    void directSendReturnsSentEnvelope(MockServer mockServer) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mockServer.getUrl()
                        + "/api/v1/notifications?subscriberMsisdn=94771234567&channel=SMS&templateKey=welcome-sms"))
                .header("X-Tenant-Id", "acme-telecom")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"success\":true");
        assertThat(response.body()).contains("\"status\":\"SENT\"");
        assertThat(response.body()).contains("\"sourceEvent\":\"direct-api\"");
    }
}
