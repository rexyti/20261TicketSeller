package com.ticketseller.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearCortesiaRequest(
        @NotBlank(message = "El destinatario es obligatorio")
        String destinatario,
        @NotNull(message = "La categoría es obligatoria")
        String categoria,
        UUID asientoId,
        UUID zonaId
) {}
