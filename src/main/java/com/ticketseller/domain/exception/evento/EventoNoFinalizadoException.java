package com.ticketseller.domain.exception.evento;

public class EventoNoFinalizadoException extends RuntimeException {
    public EventoNoFinalizadoException(String message) {
        super(message);
    }
}
