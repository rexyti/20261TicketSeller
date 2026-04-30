package com.ticketseller.domain.model.venta;

import java.util.Set;

public enum EstadoVenta {
    PENDIENTE,
    RESERVADA,
    COMPLETADA,
    EXPIRADA,
    REEMBOLSADA,
    FALLIDA;

    public Set<EstadoVenta> transicionesPermitidas() {
        return switch (this) {
            case PENDIENTE -> Set.of(RESERVADA, EXPIRADA, FALLIDA);
            case RESERVADA -> Set.of(COMPLETADA, EXPIRADA, FALLIDA);
            case COMPLETADA -> Set.of(REEMBOLSADA);
            default -> Set.of();
        };
    }
}

