package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.MecanismoAplicacion;
import com.ticketseller.domain.model.promocion.TipoUsuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record PromocionResponse(
        UUID id,
        String nombre,
        MecanismoAplicacion mecanismo,
        UUID eventoId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoPromocion estado,
        TipoUsuario tipoUsuarioRestringido
) {
}
