package com.ticketseller.domain.exception;

import java.util.UUID;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException(UUID ventaId) {
        super("La reserva ha expirado para la venta: " + ventaId);
    }
}
