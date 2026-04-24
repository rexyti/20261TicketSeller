package com.ticketseller.domain.exception;

public class TipoAsientoNotFoundException extends RuntimeException {
    public TipoAsientoNotFoundException(String message) {
        super(message);
    }
}
