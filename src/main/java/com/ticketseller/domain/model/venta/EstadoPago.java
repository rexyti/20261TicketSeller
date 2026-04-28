package com.ticketseller.domain.model.venta;

public enum EstadoPago {
    APROBADO,
    RECHAZADO,
    ERROR;

    public static EstadoPago fromValor(String valor) {
        if (noValue(valor)) return ERROR;
        return EstadoPago.valueOf(valor.trim().toUpperCase());
    }

    private static boolean noValue(String valor) {
        return valor == null || valor.isBlank();
    }
}

