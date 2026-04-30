package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

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

