package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import jakarta.validation.constraints.NotBlank;

public record EditarBloqueoRequest(
        @NotBlank String destinatario
) {
}
