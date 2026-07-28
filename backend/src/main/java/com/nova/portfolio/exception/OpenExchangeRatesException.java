package com.nova.portfolio.exception;

public class OpenExchangeRatesException extends RuntimeException {

    private final int statusCode;

    public OpenExchangeRatesException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public OpenExchangeRatesException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

