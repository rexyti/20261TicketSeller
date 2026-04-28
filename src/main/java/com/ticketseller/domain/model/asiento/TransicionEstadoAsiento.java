package com.ticketseller.domain.model.asiento;

import java.util.Map;
import java.util.Set;

public class TransicionEstadoAsiento {

    private static final Map<EstadoAsiento, Set<EstadoAsiento>> TRANSICIONES_PERMITIDAS = Map.of(
            EstadoAsiento.DISPONIBLE, Set.of(EstadoAsiento.BLOQUEADO, EstadoAsiento.RESERVADO, EstadoAsiento.MANTENIMIENTO),
            EstadoAsiento.BLOQUEADO, Set.of(EstadoAsiento.DISPONIBLE, EstadoAsiento.MANTENIMIENTO),
            EstadoAsiento.RESERVADO, Set.of(EstadoAsiento.DISPONIBLE, EstadoAsiento.VENDIDO),
            EstadoAsiento.VENDIDO, Set.of(),
            EstadoAsiento.MANTENIMIENTO, Set.of(EstadoAsiento.DISPONIBLE, EstadoAsiento.BLOQUEADO),
            EstadoAsiento.ANULADO, Set.of()
    );

    private TransicionEstadoAsiento() {
        // Utils class
    }

    public static boolean esPermitida(EstadoAsiento origen, EstadoAsiento destino) {
        if (origen == null || destino == null) {
            return false;
        }
        return TRANSICIONES_PERMITIDAS.getOrDefault(origen, Set.of()).contains(destino);
    }
}
