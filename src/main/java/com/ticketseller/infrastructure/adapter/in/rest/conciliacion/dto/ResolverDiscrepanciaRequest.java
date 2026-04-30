package com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResolverDiscrepanciaRequest(
        @NotNull(message = "confirmar es obligatorio")
        Boolean confirmar,
        @NotBlank(message = "justificacion es obligatoria")
        String justificacion,
        @NotNull(message = "agenteId es obligatorio")
        UUID agenteId
) {
}
