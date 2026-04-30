package com.ticketseller.infrastructure.adapter.in.rest.recinto.dto;

public record EditarRecintoRequest(
        String nombre,
        String ciudad,
        String direccion,
        Integer capacidadMaxima,
        String telefono,
        Integer compuertasIngreso
) {
}

