package com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto;

import com.ticketseller.domain.model.recinto.ModeloNegocio;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfigurarModeloNegocioRequest(
        @NotNull ModeloNegocio modelo,
        BigDecimal montoFijo
) {
}
