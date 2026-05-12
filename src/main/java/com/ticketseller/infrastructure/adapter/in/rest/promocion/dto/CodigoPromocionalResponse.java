package com.ticketseller.infrastructure.adapter.in.rest.promocion.dto;

import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;

import java.time.LocalDateTime;

public record CodigoPromocionalResponse(
        String codigo,
        Integer usosMaximos,
        int usosActuales,
        LocalDateTime fechaFin,
        EstadoCodigoPromocional estado
) {
}
