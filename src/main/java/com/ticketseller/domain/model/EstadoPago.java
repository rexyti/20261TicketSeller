package com.ticketseller.domain.model;

public enum EstadoPago {
    APROBADO,
    RECHAZADO,
    ERROR;

    public static EstadoPago fromValor(String valor) {
        if (valor == null || valor.isBlank()) {
            return ERROR;
        }
        try {
            return EstadoPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ERROR;
        }
    }
}

