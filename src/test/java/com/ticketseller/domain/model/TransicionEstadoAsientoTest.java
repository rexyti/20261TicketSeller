package com.ticketseller.domain.model;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransicionEstadoAsientoTest {

    @Test
    void debePermitirTransicionDeDisponibleAInactivo() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.DISPONIBLE).build();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.INACTIVO));
    }

    @Test
    void debePermitirTransicionDeDisponibleAMantenimiento() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.DISPONIBLE).build();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.MANTENIMIENTO));
    }

    @Test
    void noDebePermitirTransicionDeInactivoADisponible() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.INACTIVO).build();
        assertFalse(asiento.esTransicionPermitida(EstadoAsiento.DISPONIBLE));
    }

    @Test
    void debePermitirTransicionDeMantenimientoADisponible() {
        Asiento asiento = Asiento.builder().estado(EstadoAsiento.MANTENIMIENTO).fila("A").columna(10).build().normalizarDatosRegistro();
        assertTrue(asiento.esTransicionPermitida(EstadoAsiento.DISPONIBLE));
    }
}
