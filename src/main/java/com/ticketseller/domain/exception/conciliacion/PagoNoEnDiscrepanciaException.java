package com.ticketseller.domain.exception.conciliacion;

public class PagoNoEnDiscrepanciaException extends RuntimeException {
    public PagoNoEnDiscrepanciaException(String message) {
        super(message);
    }
}
