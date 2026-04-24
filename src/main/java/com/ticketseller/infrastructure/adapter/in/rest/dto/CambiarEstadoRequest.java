package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoAsiento;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado destino es obligatorio")
        EstadoAsiento estadoDestino,
        String motivo
) {
}
