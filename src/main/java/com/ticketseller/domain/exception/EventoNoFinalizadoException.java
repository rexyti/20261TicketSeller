package com.ticketseller.domain.exception;

public class EventoNoFinalizadoException extends RuntimeException {
    public EventoNoFinalizadoException(String message) {
        super(message);
    }
}
