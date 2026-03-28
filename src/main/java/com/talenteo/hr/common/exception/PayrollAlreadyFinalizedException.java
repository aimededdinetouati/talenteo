package com.talenteo.hr.common.exception;

public class PayrollAlreadyFinalizedException extends RuntimeException {
    public PayrollAlreadyFinalizedException(String message) {
        super(message);
    }
}
