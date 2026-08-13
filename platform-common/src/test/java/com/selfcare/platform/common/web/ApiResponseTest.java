package com.selfcare.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void ok_wrapsSuccessfulDataWithATimestamp() {
        ApiResponse<String> response = ApiResponse.ok("value");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("value");
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void failure_wrapsErrorsWithATimestamp() {
        ApiResponse.ApiError error = new ApiResponse.ApiError("BAD_REQUEST", "host is malformed", "trace-123");

        ApiResponse<Void> response = ApiResponse.failure(error);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error()).isEqualTo(error);
        assertThat(response.timestamp()).isNotNull();
    }
}
