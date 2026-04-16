package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.CategoriaRecinto;

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

