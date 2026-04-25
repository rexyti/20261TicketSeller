package com.ticketseller.domain.model;

public enum TipoReembolso {
    TOTAL,
    PARCIAL;

    public static TipoReembolso fromValor(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return TipoReembolso.valueOf(valor.trim().toUpperCase());
    }
}
