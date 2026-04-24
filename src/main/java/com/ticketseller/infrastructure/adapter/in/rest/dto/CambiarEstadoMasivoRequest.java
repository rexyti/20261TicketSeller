package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoAsiento;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoMasivoRequest(
        @NotEmpty(message = "La lista de asientos no puede estar vacía")
        List<UUID> asientoIds,
        @NotNull(message = "El estado destino es obligatorio")
        EstadoAsiento estadoDestino,
        String motivo
) {
}
