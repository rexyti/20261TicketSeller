package com.ticketseller.domain.exception.asiento;

public class AsientoNotFoundException extends RuntimeException {
    public AsientoNotFoundException(String message) {
        super(message);
    }
}
