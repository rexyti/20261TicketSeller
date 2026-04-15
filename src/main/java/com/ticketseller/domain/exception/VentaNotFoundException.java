package com.ticketseller.domain.exception;

import java.util.UUID;

public class VentaNotFoundException extends RuntimeException {
    public VentaNotFoundException(UUID ventaId) {
        super("Venta no encontrada con ID: " + ventaId);
    }
}
