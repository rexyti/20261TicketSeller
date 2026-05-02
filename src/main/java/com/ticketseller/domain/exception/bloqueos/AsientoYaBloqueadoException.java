package com.ticketseller.domain.exception.bloqueos;

import java.util.UUID;

public class AsientoYaBloqueadoException extends RuntimeException {
    public AsientoYaBloqueadoException(UUID asientoId) {
        super("El asiento %s ya se encuentra bloqueado".formatted(asientoId));
    }
}
