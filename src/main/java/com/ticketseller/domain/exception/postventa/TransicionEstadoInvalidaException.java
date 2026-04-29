package com.ticketseller.domain.exception.postventa;

import com.ticketseller.domain.model.ticket.EstadoTicket;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoTicket origen, EstadoTicket destino) {
        super("Transición de estado inválida: de %s a %s no permitida.".formatted(origen, destino));
    }
}

