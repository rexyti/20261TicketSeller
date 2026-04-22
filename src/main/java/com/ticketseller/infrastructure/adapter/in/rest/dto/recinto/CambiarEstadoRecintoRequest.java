package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRecintoRequest(@NotNull Boolean activo) {
}

