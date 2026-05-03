package com.ticketseller.domain.exception.evento;

public class EventoCanceladoSinMotivoException extends RuntimeException {
    public EventoCanceladoSinMotivoException(String message) {
        super(message);
    }
}
