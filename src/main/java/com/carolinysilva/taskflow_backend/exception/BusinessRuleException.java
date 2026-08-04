package com.carolinysilva.taskflow_backend.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
