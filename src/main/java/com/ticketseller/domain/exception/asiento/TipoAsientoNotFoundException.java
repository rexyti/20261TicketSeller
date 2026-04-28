package com.ticketseller.domain.exception.asiento;

public class TipoAsientoNotFoundException extends RuntimeException {
    public TipoAsientoNotFoundException(String message) {
        super(message);
    }
}
