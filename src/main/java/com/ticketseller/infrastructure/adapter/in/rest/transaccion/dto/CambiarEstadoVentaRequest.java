package com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto;

import com.ticketseller.domain.model.venta.EstadoVenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CambiarEstadoVentaRequest(
        @NotNull(message = "nuevoEstado es obligatorio")
        EstadoVenta nuevoEstado,
        @NotBlank(message = "justificacion es obligatoria")
        String justificacion,
        UUID actorId
) {
}
