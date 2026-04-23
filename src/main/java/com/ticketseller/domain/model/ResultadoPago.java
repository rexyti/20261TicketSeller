package com.ticketseller.domain.model;

public record ResultadoPago(
        boolean aprobado,
        String estadoPago,
        String codigoAutorizacion,
        String respuestaPasarela
) {
}

