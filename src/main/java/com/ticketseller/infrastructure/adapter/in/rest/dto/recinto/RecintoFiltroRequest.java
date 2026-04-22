package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

public record RecintoFiltroRequest(
        String nombre,
        String ciudad,
        String categoria,
        String estado,
        Integer page,
        Integer size,
        String sort
) {
}

