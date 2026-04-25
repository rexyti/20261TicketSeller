package com.ticketseller.domain.model;

public enum EstadoReembolso {
    PENDIENTE,
    EN_PROCESO,
    COMPLETADO,
    FALLIDO;

    public static EstadoReembolso fromValor(String valor) {
        if (valor == null || valor.isBlank()) return PENDIENTE;
        return EstadoReembolso.valueOf(valor.trim().toUpperCase());
    }
}
