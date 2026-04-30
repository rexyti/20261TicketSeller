package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import jakarta.validation.constraints.NotNull;

public record ConfigurarCategoriaRequest(@NotNull CategoriaRecinto categoria) {
}

