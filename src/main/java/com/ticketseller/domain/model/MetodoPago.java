package com.ticketseller.domain.model;

public enum MetodoPago {
    TARJETA,
    TRANSFERENCIA,
    OTRO;

    public static MetodoPago fromValor(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El metodo de pago es obligatorio");
        }
        try {
            return MetodoPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Metodo de pago no soportado: " + valor);
        }
    }
}

