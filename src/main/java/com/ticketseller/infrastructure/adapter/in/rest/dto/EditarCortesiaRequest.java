package com.ticketseller.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record EditarCortesiaRequest(
        @NotBlank(message = "El destinatario es obligatorio")
        String destinatario
) {}
