package com.ticketseller.domain.model.ticket;

import java.util.Set;

public enum EstadoTicket {
    USADO,
    VENDIDO,
    CANCELADO,
    REEMBOLSO_PENDIENTE,
    ANULADO,
    REEMBOLSADO;

    public Set<EstadoTicket> transicionesPermitidas() {
        return switch (this) {
            case VENDIDO -> Set.of(CANCELADO, ANULADO, REEMBOLSO_PENDIENTE);
            case CANCELADO -> Set.of(REEMBOLSO_PENDIENTE, REEMBOLSADO, ANULADO, VENDIDO);
            case REEMBOLSO_PENDIENTE -> Set.of(REEMBOLSADO, ANULADO);
            case ANULADO -> Set.of(VENDIDO);
            default -> Set.of();
        };
    }
}
