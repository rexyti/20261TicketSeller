package com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.ModeloNegocio;

import java.math.BigDecimal;
import java.util.UUID;

public record ModeloNegocioResponse(
        UUID recintoId,
        ModeloNegocio modelo,
        CategoriaRecinto tipoRecinto,
        BigDecimal montoFijo
) {
}
