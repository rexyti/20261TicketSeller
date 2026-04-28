package com.ticketseller.domain;

import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.model.zona.Compuerta;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompuertaTest {

    @Test
    void deberiaConstruirCompuerta() {
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).nombre("Puerta A").esGeneral(false).build();

        assertFalse(compuerta.isEsGeneral());
    }

    @Test
    void deberiaNormalizarNombreEnCompuerta() {
        Compuerta compuerta = Compuerta.builder().nombre("  Puerta A  ").build();

        Compuerta normalizada = compuerta.normalizarDatosRegistro();

        assertEquals("Puerta A", normalizada.getNombre());
    }

    @Test
    void deberiaFallarCuandoNombreCompuertaEsInvalido() {
        Compuerta compuerta = Compuerta.builder().nombre(" ").build();

        assertThrows(CompuertaInvalidaException.class, compuerta::validarDatosRegistro);
    }
}

