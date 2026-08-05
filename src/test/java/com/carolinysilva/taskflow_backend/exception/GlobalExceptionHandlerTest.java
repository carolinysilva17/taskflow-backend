package com.carolinysilva.taskflow_backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/some-path");
        return request;
    }

    @Test
    void handleApiException_shouldUseTheStatusDeclaredByEachExceptionType() {
        assertStatusAndErrorCode(
                new ResourceNotFoundException("NOT_FOUND_CODE", "not found"), HttpStatus.NOT_FOUND, "NOT_FOUND_CODE");
        assertStatusAndErrorCode(
                new BusinessRuleException("CONFLICT_CODE", "conflict"), HttpStatus.CONFLICT, "CONFLICT_CODE");
        assertStatusAndErrorCode(
                new InvalidCredentialsException("CREDENTIALS_CODE", "bad credentials"), HttpStatus.UNAUTHORIZED, "CREDENTIALS_CODE");
        assertStatusAndErrorCode(
                new InvalidTokenException("TOKEN_CODE", "bad token"), HttpStatus.UNAUTHORIZED, "TOKEN_CODE");
        assertStatusAndErrorCode(
                new RateLimitExceededException("RATE_CODE", "too many"), HttpStatus.TOO_MANY_REQUESTS, "RATE_CODE");
    }

    private void assertStatusAndErrorCode(ApiException ex, HttpStatus expectedStatus, String expectedErrorCode) {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody().errorCode()).isEqualTo(expectedErrorCode);
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleDataIntegrityViolation_shouldReturn409WithGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo("DATA_CONFLICT");
    }
}
