package com.ticketseller.domain.exception.transaccion;

import com.ticketseller.domain.model.venta.EstadoVenta;

public class TransicionVentaInvalidaException extends RuntimeException {
    public TransicionVentaInvalidaException(EstadoVenta origen, EstadoVenta destino) {
        super("Transición inválida de venta: de %s a %s no está permitida.".formatted(origen, destino));
    }

    public TransicionVentaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
