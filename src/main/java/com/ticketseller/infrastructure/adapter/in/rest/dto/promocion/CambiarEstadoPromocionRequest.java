package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPromocionRequest(
        @NotNull EstadoPromocion estado
) {
}

