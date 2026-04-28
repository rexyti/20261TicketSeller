package com.ticketseller.domain.exception.asiento;

public class TipoAsientoInvalidoException extends RuntimeException {
    public TipoAsientoInvalidoException(String message) {
        super(message);
    }
}
