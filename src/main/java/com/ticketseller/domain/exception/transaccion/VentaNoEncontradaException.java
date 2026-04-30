package com.ticketseller.domain.exception.transaccion;

import java.util.UUID;

public class VentaNoEncontradaException extends RuntimeException {
    public VentaNoEncontradaException(UUID id) {
        super("Venta no encontrada: " + id);
    }
}
