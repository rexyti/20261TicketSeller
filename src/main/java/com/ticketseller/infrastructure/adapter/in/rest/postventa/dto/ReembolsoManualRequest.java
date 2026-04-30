package com.ticketseller.infrastructure.adapter.in.rest.postventa.dto;

import com.ticketseller.domain.model.postventa.TipoReembolso;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ReembolsoManualRequest(
        @NotNull(message = "tipo es obligatorio")
        TipoReembolso tipo,
        BigDecimal monto,
        UUID agenteId
) {
}

