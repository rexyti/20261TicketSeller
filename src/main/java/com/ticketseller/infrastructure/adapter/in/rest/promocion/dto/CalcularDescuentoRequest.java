package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.TipoUsuario;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CalcularDescuentoRequest(
        @NotNull UUID eventoId,
        TipoUsuario tipoUsuario,
        @NotEmpty List<ItemCarritoDto> items
) {
    public record ItemCarritoDto(@NotNull UUID zonaId, @NotNull BigDecimal precio) {
    }
}
