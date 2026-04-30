package com.ticketseller.infrastructure.adapter.in.rest.dto.checkout;

import com.ticketseller.domain.model.promocion.TipoUsuario;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservarAsientosRequest(
        @NotNull UUID compradorId,
        @NotNull UUID eventoId,
        @NotNull UUID zonaId,
        @NotNull @Min(1) Integer cantidad,
        Boolean esCortesia,
        TipoUsuario tipoUsuario
) {
}

