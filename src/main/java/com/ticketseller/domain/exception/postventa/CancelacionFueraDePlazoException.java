package com.ticketseller.domain.exception.postventa;

public class CancelacionFueraDePlazoException extends RuntimeException {
    public CancelacionFueraDePlazoException(String message) {
        super(message);
    }
}

