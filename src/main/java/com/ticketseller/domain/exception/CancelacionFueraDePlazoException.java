package com.ticketseller.domain.exception;

public class CancelacionFueraDePlazoException extends RuntimeException {
    public CancelacionFueraDePlazoException(String message) {
        super(message);
    }
}
