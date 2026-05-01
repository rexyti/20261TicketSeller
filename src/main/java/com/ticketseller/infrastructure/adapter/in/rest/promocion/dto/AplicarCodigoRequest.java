package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import jakarta.validation.constraints.NotBlank;

public record AplicarCodigoRequest(@NotBlank String codigo) {
}
