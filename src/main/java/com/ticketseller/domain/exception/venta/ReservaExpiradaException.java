package com.ticketseller.domain.exception.venta;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException(String message) {
        super(message);
    }
}

