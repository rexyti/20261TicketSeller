package com.ticketseller.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigurarCapacidadRequest(@NotNull @Min(1) Integer capacidadMaxima) {
}

