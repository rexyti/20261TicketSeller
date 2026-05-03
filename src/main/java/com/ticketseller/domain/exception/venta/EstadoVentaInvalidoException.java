package com.ticketseller.domain.exception.venta;

public class EstadoVentaInvalidoException extends RuntimeException {
    public EstadoVentaInvalidoException(String message) {
        super(message);
    }
}
