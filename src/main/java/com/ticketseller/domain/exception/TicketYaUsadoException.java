package com.ticketseller.domain.exception;

import java.util.UUID;

public class TicketYaUsadoException extends RuntimeException {
    public TicketYaUsadoException(UUID ticketId) {
        super("El ticket con ID " + ticketId + " ya ha sido usado y no puede ser cancelado.");
    }
}
