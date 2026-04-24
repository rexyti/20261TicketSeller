package com.ticketseller.domain.model;

public enum MetodoPago {
    TARJETA,
    PSE,
    NEQUI,
    DAVIPLATA,
    OTRO;

    public static MetodoPago fromValor(String valor) {
        if (noValue(valor)) throw new IllegalArgumentException("El método de pago es obligatorio");
        return MetodoPago.valueOf(valor.trim().toUpperCase());
    }

    private static boolean noValue(String valor) {
        return valor == null || valor.isBlank();
    }
}
