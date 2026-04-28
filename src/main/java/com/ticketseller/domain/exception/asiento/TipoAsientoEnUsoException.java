package com.ticketseller.domain.exception.asiento;

public class TipoAsientoEnUsoException extends RuntimeException {
    public TipoAsientoEnUsoException(String message) {
        super(message);
    }
}
