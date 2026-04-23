package com.ticketseller.infrastructure.adapter.in.rest.dto.evento;

import jakarta.validation.constraints.NotBlank;

public record CancelarEventoRequest(
        @NotBlank String motivo
) {
}

