package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.TipoReembolso;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReembolsoManualRequest(
    @NotNull(message = "El tipo de reembolso es obligatorio")
    TipoReembolso tipo,
    
    BigDecimal monto
) {}
