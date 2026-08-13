package com.selfcare.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NotFoundExceptionTest {

    @Test
    void constructor_setsTheStandardNotFoundErrorCodeAndStatus() {
        NotFoundException exception = new NotFoundException("tenant not found");

        assertThat(exception.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("tenant not found");
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
