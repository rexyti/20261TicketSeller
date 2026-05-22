package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AplicarCodigoRequest(@NotBlank String codigo) {
}
