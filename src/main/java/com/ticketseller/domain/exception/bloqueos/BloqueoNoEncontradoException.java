package com.ticketseller.domain.exception.bloqueos;

import java.util.UUID;

public class BloqueoNoEncontradoException extends RuntimeException {
    public BloqueoNoEncontradoException(UUID bloqueoId) {
        super("No se encontró el bloqueo con id %s".formatted(bloqueoId));
    }
}
