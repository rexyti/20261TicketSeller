package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoReembolso;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReembolsoResponse(
    UUID reembolsoId,
    EstadoReembolso estado,
    BigDecimal monto,
    UUID agenteId,
    LocalDateTime fechaCompletado
) {}
