package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AsignarAsientosAZonaRequest(
        @NotEmpty List<UUID> asientoIds
) {
}
