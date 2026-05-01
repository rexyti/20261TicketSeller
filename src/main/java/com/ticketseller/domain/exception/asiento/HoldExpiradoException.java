package com.ticketseller.domain.exception.asiento;

public class HoldExpiradoException extends RuntimeException {
    public HoldExpiradoException(String message) {
        super(message);
    }
}
