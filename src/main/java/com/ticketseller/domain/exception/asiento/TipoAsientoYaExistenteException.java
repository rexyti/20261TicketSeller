package com.ticketseller.domain.exception.asiento;

public class TipoAsientoYaExistenteException extends RuntimeException {
    public TipoAsientoYaExistenteException(String message) {
        super(message);
    }
}
