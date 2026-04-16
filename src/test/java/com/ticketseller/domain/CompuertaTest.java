package com.ticketseller.domain;

import com.ticketseller.domain.model.Compuerta;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CompuertaTest {

    @Test
    void deberiaConstruirCompuerta() {
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta A").esGeneral(false).build();

        assertFalse(compuerta.isEsGeneral());
    }
}

