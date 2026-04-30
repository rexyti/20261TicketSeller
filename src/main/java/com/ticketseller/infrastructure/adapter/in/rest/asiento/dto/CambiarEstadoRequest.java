package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import com.ticketseller.domain.model.asiento.EstadoAsiento;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado destino es obligatorio")
        EstadoAsiento estadoDestino,
        String motivo
) {
}
