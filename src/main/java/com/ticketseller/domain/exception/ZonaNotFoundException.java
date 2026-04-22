package com.ticketseller.domain.exception;

public class ZonaNotFoundException extends RuntimeException {
    public ZonaNotFoundException(String message) {
        super(message);
    }
}
