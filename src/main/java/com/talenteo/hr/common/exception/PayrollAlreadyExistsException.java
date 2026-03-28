package com.talenteo.hr.common.exception;

public class PayrollAlreadyExistsException extends RuntimeException {
    public PayrollAlreadyExistsException(String message) {
        super(message);
    }
}
