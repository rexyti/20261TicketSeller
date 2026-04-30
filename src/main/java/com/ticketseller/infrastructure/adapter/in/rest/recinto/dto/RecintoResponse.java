package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RecintoResponse(
        UUID id,
        String nombre,
        String ciudad,
        String direccion,
        Integer capacidadMaxima,
        String telefono,
        LocalDateTime fechaCreacion,
        Integer compuertasIngreso,
        boolean activo,
        CategoriaRecinto categoria
) {
}

