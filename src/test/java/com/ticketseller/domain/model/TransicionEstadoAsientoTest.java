package com.ticketseller.domain.model;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransicionEstadoAsientoTest {

    @Test
    void debePermitirTransicionDeDisponibleABloqueado() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.DISPONIBLE).build();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.BLOQUEADO));
    }

    @Test
    void debePermitirTransicionDeDisponibleAMantenimiento() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.DISPONIBLE).build();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.MANTENIMIENTO));
    }

    @Test
    void noDebePermitirTransicionDeVendidoADisponible() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.VENDIDO).build();
        assertFalse(asiento.esTransicionPermitida(EstadoAsiento.DISPONIBLE));
    }

    @Test
    void debePermitirTransicionDeBloqueadoADisponible() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.BLOQUEADO).build();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.DISPONIBLE));
    }
}
