package com.selfcare.loyalty.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuthServiceCustomerValidationClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsTrueWhenAuthServiceMarksCustomerAsValid() throws Exception {
        AtomicReference<String> observedQuery = new AtomicReference<>();
        startServer(exchange -> {
            observedQuery.set(exchange.getRequestURI().getQuery());
            writeResponse(exchange, 200, "{\"valid\":true}");
        });

        AuthServiceCustomerValidationClient client = new AuthServiceCustomerValidationClient(baseUrl());

        assertThat(client.isValidCustomer("912345678V", "94771234567")).isTrue();
        assertThat(observedQuery.get()).contains("nationalId=912345678V").contains("msisdn=94771234567");
    }

    @Test
    void returnsFalseWhenAuthServiceRespondsWithValidFalse() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, "{\"valid\":false}"));

        AuthServiceCustomerValidationClient client = new AuthServiceCustomerValidationClient(baseUrl());

        assertThat(client.isValidCustomer("912345678V", "94771234567")).isFalse();
    }

    @Test
    void returnsFalseWhenAuthServiceReturnsANullBody() throws Exception {
        startServer(exchange -> writeResponse(exchange, 200, "null"));

        AuthServiceCustomerValidationClient client = new AuthServiceCustomerValidationClient(baseUrl());

        assertThat(client.isValidCustomer("912345678V", "94771234567")).isFalse();
    }

    @Test
    void returnsFalseWhenAuthServiceIsUnreachable() {
        AuthServiceCustomerValidationClient client = new AuthServiceCustomerValidationClient("http://127.0.0.1:1");

        assertThat(client.isValidCustomer("912345678V", "94771234567")).isFalse();
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/customers/validate", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void writeResponse(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }
}
