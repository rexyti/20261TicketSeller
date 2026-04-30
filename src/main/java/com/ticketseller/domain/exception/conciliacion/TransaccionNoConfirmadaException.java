package com.ticketseller.domain.exception.conciliacion;

import java.util.UUID;

public class TransaccionNoConfirmadaException extends RuntimeException {
    public TransaccionNoConfirmadaException(UUID ventaId) {
        super("No existe un pago confirmado para la venta: " + ventaId);
    }
}
