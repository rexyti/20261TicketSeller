package com.ticketseller.domain;

import com.ticketseller.domain.model.Zona;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZonaTest {

    @Test
    void deberiaConstruirZona() {
        UUID recintoId = UUID.randomUUID();
        Zona zona = Zona.builder().id(UUID.randomUUID()).recintoId(recintoId).nombre("VIP").capacidad(200).build();

        assertEquals(recintoId, zona.getRecintoId());
        assertEquals("VIP", zona.getNombre());
    }
}

