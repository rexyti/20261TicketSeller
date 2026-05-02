package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BloquearAsientosRequest(
        @NotEmpty List<UUID> asientoIds,
        @NotBlank String destinatario,
        LocalDateTime fechaExpiracion
) {
}
