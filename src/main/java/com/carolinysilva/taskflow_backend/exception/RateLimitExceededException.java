package com.carolinysilva.taskflow_backend.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(String errorCode, String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, errorCode, message);
    }
}
