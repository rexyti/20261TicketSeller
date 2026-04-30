package com.ticketseller.infrastructure.adapter.in.rest.evento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ConfigurarPreciosRequest(
        @NotEmpty List<@Valid PrecioZonaRequest> precios
) {
}

