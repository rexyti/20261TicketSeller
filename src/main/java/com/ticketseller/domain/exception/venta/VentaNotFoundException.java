package com.ticketseller.domain.exception.venta;

public class VentaNotFoundException extends RuntimeException {
    public VentaNotFoundException(String message) {
        super(message);
    }
}

