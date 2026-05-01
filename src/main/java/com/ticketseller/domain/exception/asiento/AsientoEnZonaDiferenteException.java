package com.ticketseller.domain.exception.asiento;

public class AsientoEnZonaDiferenteException extends RuntimeException {
    public AsientoEnZonaDiferenteException(String message) {
        super(message);
    }
}
