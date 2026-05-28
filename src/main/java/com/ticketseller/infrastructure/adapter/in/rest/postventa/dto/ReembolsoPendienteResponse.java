package com.ticketseller.infrastructure.adapter.in.rest.postventa.dto;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReembolsoPendienteResponse(
        UUID reembolsoId,
        UUID ticketId,
        UUID ventaId,
        BigDecimal monto,
        TipoReembolso tipo,
        EstadoReembolso estado,
        LocalDateTime fechaSolicitud,
        UUID agenteId
) {
}
