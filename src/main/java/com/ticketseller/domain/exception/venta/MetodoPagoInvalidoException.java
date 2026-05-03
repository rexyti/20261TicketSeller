package com.ticketseller.domain.exception.venta;

public class MetodoPagoInvalidoException extends RuntimeException {
    public MetodoPagoInvalidoException(String message) {
        super(message);
    }
}
