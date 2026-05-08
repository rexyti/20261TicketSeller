package com.ticketseller.infrastructure.adapter.in.rest.zona.dto;

import com.ticketseller.domain.model.zona.TipoZona;
import java.util.UUID;

public record ZonaResponse(
                UUID id,
                UUID recintoId,
                String nombre,
                Integer capacidad,
                UUID tipoAsientoId,
                TipoZona tipo) {
}
