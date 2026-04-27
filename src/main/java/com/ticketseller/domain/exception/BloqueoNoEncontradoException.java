package com.ticketseller.domain.exception;

public class BloqueoNoEncontradoException extends RuntimeException {
    public BloqueoNoEncontradoException(String message) {
        super(message);
    }
}
