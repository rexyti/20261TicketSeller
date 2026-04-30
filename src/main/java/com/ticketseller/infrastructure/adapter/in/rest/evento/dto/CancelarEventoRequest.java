package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelarEventoRequest(
        @NotBlank String motivo
) {
}

