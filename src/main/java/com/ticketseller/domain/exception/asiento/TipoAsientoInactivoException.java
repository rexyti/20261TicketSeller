package com.ticketseller.domain.exception.asiento;

public class TipoAsientoInactivoException extends RuntimeException {
    public TipoAsientoInactivoException(String message) {
        super(message);
    }
}
