package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearPromocionRequest(
        @NotBlank String nombre,
        @NotNull TipoPromocion tipo,
        @NotNull UUID eventoId,
        @NotNull LocalDateTime fechaInicio,
        @NotNull LocalDateTime fechaFin,
        TipoUsuario tipoUsuarioRestringido
) {
}
