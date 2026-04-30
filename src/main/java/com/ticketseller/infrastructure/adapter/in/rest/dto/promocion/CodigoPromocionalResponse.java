package com.ticketseller.infrastructure.adapter.in.rest.dto.promocion;

import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;

import java.time.LocalDateTime;
import java.util.UUID;

public record CodigoPromocionalResponse(
        UUID id,
        String codigo,
        UUID promocionId,
        Integer usosMaximos,
        int usosActuales,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        EstadoCodigoPromocional estado
) {
}

