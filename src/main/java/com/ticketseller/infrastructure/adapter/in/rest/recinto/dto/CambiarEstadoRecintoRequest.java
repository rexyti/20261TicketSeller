package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRecintoRequest(@NotNull Boolean activo) {
}

