package com.ticketseller.infrastructure.adapter.in.rest.dto.recinto;

public record EditarRecintoRequest(
        String nombre,
        String ciudad,
        String direccion,
        Integer capacidadMaxima,
        String telefono,
        Integer compuertasIngreso
) {
}

