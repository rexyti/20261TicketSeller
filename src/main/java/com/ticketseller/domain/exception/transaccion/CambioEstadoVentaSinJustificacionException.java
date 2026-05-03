package com.ticketseller.domain.exception.transaccion;

public class CambioEstadoVentaSinJustificacionException extends RuntimeException {
    public CambioEstadoVentaSinJustificacionException(String message) {
        super(message);
    }
}
