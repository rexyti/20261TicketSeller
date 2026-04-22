package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

import com.ticketseller.domain.model.CategoriaRecinto;
import jakarta.validation.constraints.NotNull;

public record ConfigurarCategoriaRequest(@NotNull CategoriaRecinto categoria) {
}

