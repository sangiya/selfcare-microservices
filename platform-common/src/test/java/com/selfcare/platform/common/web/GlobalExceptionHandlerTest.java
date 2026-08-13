package com.selfcare.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Pins the status code a malformed request gets. These used to fall through to the catch-all
 * handler and come back as 500 INTERNAL_ERROR, which blamed the service for the caller's mistake
 * and counted against the error rate on-call alerting watches.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingRequiredParameterIsRejectedAsBadRequest() throws Exception {
        mvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("msisdn is required"));
    }

    @Test
    void unconvertibleParameterIsRejectedAsBadRequest() throws Exception {
        mvc.perform(get("/probe/enum").param("channel", "CARRIER_PIGEON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("channel has an invalid value"));
    }

    @Test
    void unreadableBodyIsRejectedAsBadRequestWithoutLeakingParserInternals() throws Exception {
        mvc.perform(post("/probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"CARRIER_PIGEON\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Request body is malformed or contains an invalid value"));
    }

    @Test
    void genuinelyUnexpectedFailuresAreStillReportedAsServerErrors() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred"));
    }

    @Test
    void apiExceptionsKeepTheirDeclaredStatusAndErrorCode() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleApiException(
                new BadRequestException("host is malformed"),
                new MockHttpServletRequest("GET", "/probe/api"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().error().message()).isEqualTo("host is malformed");
    }

    @Test
    void methodArgumentValidationErrorsAreCollapsedIntoOneClientMessage() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ProbeController.Payload(null), "payload");
        bindingResult.addError(new FieldError("payload", "channel", "channel is required"));
        bindingResult.addError(new FieldError("payload", "language", "language is required"));

        Method method = ProbeController.class.getDeclaredMethod("body", ProbeController.Payload.class);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().error().message())
                .isEqualTo("channel is required; language is required");
    }

    @Test
    void handlerMethodValidationErrorsAreAlsoReportedAsBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        MessageSourceResolvable tenantIdError = mock(MessageSourceResolvable.class);
        MessageSourceResolvable hostError = mock(MessageSourceResolvable.class);
        when(tenantIdError.getDefaultMessage()).thenReturn("tenantId is required");
        when(hostError.getDefaultMessage()).thenReturn("host is required");
        when(exception.getMessage()).thenReturn("validation failed");
        doReturn(java.util.List.of(tenantIdError, hostError)).when(exception).getAllErrors();

        var response = handler.handleBadRequest(exception, new MockHttpServletRequest("POST", "/probe/resolve"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("tenantId is required; host is required");
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        enum Channel {
            SMS,
            EMAIL
        }

        @GetMapping("/param")
        String requiredParam(@RequestParam String msisdn) {
            return msisdn;
        }

        @GetMapping("/enum")
        String enumParam(@RequestParam Channel channel) {
            return channel.name();
        }

        @PostMapping("/body")
        String body(@RequestBody Payload payload) {
            return payload.channel().name();
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("kaboom");
        }

        record Payload(Channel channel) {}
    }
}
