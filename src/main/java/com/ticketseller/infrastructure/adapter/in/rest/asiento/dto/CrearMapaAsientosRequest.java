package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import jakarta.validation.constraints.Positive;

public record CrearMapaAsientosRequest(
        @Positive int filas,
        @Positive int columnasPorFila
) {
}
