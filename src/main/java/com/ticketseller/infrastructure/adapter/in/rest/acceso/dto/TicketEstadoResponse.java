package com.ticketseller.infrastructure.adapter.in.rest.acceso.dto;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketEstadoResponse(
    UUID ticketId,
    UUID eventoId,
    EstadoTicket estado,
    String categoria,
    String bloque,
    String coordenadaAcceso,
    LocalDateTime fechaEvento
) {}
