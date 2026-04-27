package com.ticketseller.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BloquearAsientosRequest(
        @NotEmpty(message = "La lista de asientos no puede estar vacía")
        List<UUID> asientoIds,
        @NotBlank(message = "El destinatario es obligatorio")
        String destinatario,
        LocalDateTime fechaExpiracion
) {}
