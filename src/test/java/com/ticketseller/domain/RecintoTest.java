package com.ticketseller.domain;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecintoTest {

    @Test
    void deberiaConstruirRecinto() {
        Recinto recinto = Recinto.builder()
                .id(UUID.randomUUID())
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .activo(true)
                .categoria(CategoriaRecinto.TEATRO)
                .build();

        assertEquals("Movistar Arena", recinto.getNombre());
        assertTrue(recinto.isActivo());
        assertEquals(CategoriaRecinto.TEATRO, recinto.getCategoria());
    }
}

