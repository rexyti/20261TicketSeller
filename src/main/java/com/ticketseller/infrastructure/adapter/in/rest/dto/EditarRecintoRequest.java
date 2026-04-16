package com.ticketseller.infrastructure.adapter.in.rest.dto;

public record EditarRecintoRequest(
        String nombre,
        String ciudad,
        String direccion,
        Integer capacidadMaxima,
        String telefono,
        Integer compuertasIngreso
) {
}

