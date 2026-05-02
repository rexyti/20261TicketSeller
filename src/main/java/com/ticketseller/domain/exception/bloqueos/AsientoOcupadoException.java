package com.ticketseller.domain.exception.bloqueos;

import java.util.UUID;

public class AsientoOcupadoException extends RuntimeException {
    public AsientoOcupadoException(UUID asientoId) {
        super("El asiento %s se encuentra ocupado y no puede bloquearse".formatted(asientoId));
    }
}
