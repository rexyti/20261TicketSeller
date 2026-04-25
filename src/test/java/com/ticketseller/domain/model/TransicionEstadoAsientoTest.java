package com.ticketseller.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransicionEstadoAsientoTest {

    @Test
    void debePermitirTransicionDeDisponibleABloqueado() {
        assertTrue(TransicionEstadoAsiento.esPermitida(EstadoAsiento.DISPONIBLE, EstadoAsiento.BLOQUEADO));
    }

    @Test
    void debePermitirTransicionDeDisponibleAMantenimiento() {
        assertTrue(TransicionEstadoAsiento.esPermitida(EstadoAsiento.DISPONIBLE, EstadoAsiento.MANTENIMIENTO));
    }

    @Test
    void noDebePermitirTransicionDeVendidoADisponible() {
        assertFalse(TransicionEstadoAsiento.esPermitida(EstadoAsiento.VENDIDO, EstadoAsiento.DISPONIBLE));
    }

    @Test
    void noDebePermitirTransicionesConNulos() {
        assertFalse(TransicionEstadoAsiento.esPermitida(null, EstadoAsiento.DISPONIBLE));
        assertFalse(TransicionEstadoAsiento.esPermitida(EstadoAsiento.DISPONIBLE, null));
    }

    @Test
    void debePermitirTransicionDeBloqueadoADisponible() {
        assertTrue(TransicionEstadoAsiento.esPermitida(EstadoAsiento.BLOQUEADO, EstadoAsiento.DISPONIBLE));
    }
}
