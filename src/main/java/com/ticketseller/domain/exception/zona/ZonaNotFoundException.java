package com.ticketseller.domain.exception.zona;

public class ZonaNotFoundException extends RuntimeException {
    public ZonaNotFoundException(String message) {
        super(message);
    }
}
