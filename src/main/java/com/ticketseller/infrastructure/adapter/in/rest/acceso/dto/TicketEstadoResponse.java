package com.ticketseller.infrastructure.adapter.in.rest.acceso.dto;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketEstadoResponse(
    UUID ticketId,
    EstadoTicket estado,
    String categoria,
    String bloque,
    String coordenadaAcceso,
    UUID eventoId,
    LocalDateTime fechaEvento
) {}
