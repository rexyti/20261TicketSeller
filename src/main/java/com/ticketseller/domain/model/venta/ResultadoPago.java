package com.ticketseller.domain.model.venta;

public record ResultadoPago(
        boolean aprobado,
        String estadoPago,
        String codigoAutorizacion,
        String respuestaPasarela
) {
}

