package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;

import java.util.UUID;

public record AsientoEnEvento(
        UUID asientoId,
        String fila,
        Integer columna,
        String numero,
        UUID zonaId,
        String estadoEnEvento
) {
    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String RESERVADO = "RESERVADO";
    public static final String VENDIDO = "VENDIDO";
    public static final String BLOQUEADO = "BLOQUEADO";
    public static final String MANTENIMIENTO = "MANTENIMIENTO";
    public static final String INACTIVO = "INACTIVO";

    public static AsientoEnEvento desde(Asiento asiento, String estadoEnEvento) {
        return new AsientoEnEvento(
                asiento.getId(), asiento.getFila(), asiento.getColumna(),
                asiento.getNumero(), asiento.getZonaId(), estadoEnEvento);
    }
}
