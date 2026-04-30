package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record PromocionResponse(
        UUID id,
        String nombre,
        TipoPromocion tipo,
        UUID eventoId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoPromocion estado,
        TipoUsuario tipoUsuarioRestringido
) {
}

