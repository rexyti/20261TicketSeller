package com.ticketseller.domain.exception;

public class VentaNotFoundException extends RuntimeException {
    public VentaNotFoundException(String message) {
        super(message);
    }
}

