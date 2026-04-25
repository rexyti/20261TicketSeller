package com.ticketseller.domain.model;

public enum EstadoTicket {
    VENDIDO,
    ANULADO,
    REEMBOLSADO,
    CANCELADO,
    REEMBOLSO_PENDIENTE,
    USADO;

    public static EstadoTicket fromValor(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return EstadoTicket.valueOf(valor.trim().toUpperCase());
    }
}
