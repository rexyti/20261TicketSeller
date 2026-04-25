package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.EstadoTicket;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketConReembolsoResponse(
    UUID id,
    UUID ventaId,
    UUID eventoId,
    EstadoTicket estado,
    BigDecimal precio,
    String codigoQr,
    EstadoReembolso estadoReembolso,
    BigDecimal montoReembolso,
    LocalDateTime fechaSolicitudReembolso
) {}
