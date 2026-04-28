package com.ticketseller.domain.exception.asiento;

import com.ticketseller.domain.model.asiento.EstadoAsiento;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoAsiento origen, EstadoAsiento destino) {
        super(String.format("Transición de estado inválida: de %s a %s no permitida.", origen, destino));
    }
}
