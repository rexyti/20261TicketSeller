package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoPromocionRequest(@NotNull EstadoPromocion estado) {
}
