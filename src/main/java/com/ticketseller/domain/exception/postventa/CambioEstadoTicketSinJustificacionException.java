package com.ticketseller.domain.exception.postventa;

public class CambioEstadoTicketSinJustificacionException extends RuntimeException {
    public CambioEstadoTicketSinJustificacionException(String message) {
        super(message);
    }
}
