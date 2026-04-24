package com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion;

import com.ticketseller.domain.model.ModeloNegocio;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfigurarModeloNegocioRequest(
        @NotNull ModeloNegocio modelo,
        BigDecimal montoFijo
) {
}
