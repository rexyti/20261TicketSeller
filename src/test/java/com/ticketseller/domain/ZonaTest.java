package com.ticketseller.domain;

import com.ticketseller.domain.exception.ZonaInvalidaException;
import com.ticketseller.domain.model.Zona;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZonaTest {

    @Test
    void deberiaConstruirZona() {
        UUID recintoId = UUID.randomUUID();
        Zona zona = Zona.builder().id(UUID.randomUUID()).recintoId(recintoId).nombre("VIP").capacidad(200).build();

        assertEquals(recintoId, zona.getRecintoId());
        assertEquals("VIP", zona.getNombre());
    }

    @Test
    void deberiaNormalizarNombreEnZona() {
        Zona zona = Zona.builder().nombre("  VIP  ").capacidad(200).build();

        Zona normalizada = zona.normalizarDatosRegistro();

        assertEquals("VIP", normalizada.getNombre());
    }

    @Test
    void deberiaFallarCuandoCapacidadEsInvalida() {
        Zona zona = Zona.builder().nombre("VIP").capacidad(0).build();

        assertThrows(ZonaInvalidaException.class, zona::validarDatosRegistro);
    }
}

