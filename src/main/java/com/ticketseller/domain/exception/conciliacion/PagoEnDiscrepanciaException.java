package com.ticketseller.domain.exception.conciliacion;

import java.util.UUID;

public class PagoEnDiscrepanciaException extends RuntimeException {
    public PagoEnDiscrepanciaException(UUID pagoId) {
        super("El pago '%s' se encuentra en discrepancia y requiere resolución manual.".formatted(pagoId));
    }
}
