package com.ticketseller.domain.exception.conciliacion;

public class TransaccionDuplicadaException extends RuntimeException {
    public TransaccionDuplicadaException(String idExterno) {
        super("La transacción con id externo '%s' ya fue procesada.".formatted(idExterno));
    }
}
