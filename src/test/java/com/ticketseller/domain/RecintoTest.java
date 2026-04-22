package com.ticketseller.domain;

import com.ticketseller.domain.exception.RecintoInvalidoException;
import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void deberiaNormalizarCamposTextoEnRegistro() {
        Recinto request = Recinto.builder()
                .nombre("  Movistar Arena  ")
                .ciudad(" Bogota ")
                .direccion(" Calle 1 ")
                .telefono(" 3001234567 ")
                .capacidadMaxima(1000)
                .compuertasIngreso(4)
                .build();

        Recinto normalizado = request.normalizarDatosRegistro();

        assertEquals("Movistar Arena", normalizado.getNombre());
        assertEquals("Bogota", normalizado.getCiudad());
        assertEquals("Calle 1", normalizado.getDireccion());
        assertEquals("3001234567", normalizado.getTelefono());
    }

    @Test
    void deberiaFallarSiNombreEsInvalido() {
        Recinto request = Recinto.builder()
                .nombre(" ")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .telefono("3001234567")
                .capacidadMaxima(1000)
                .compuertasIngreso(4)
                .build();

        assertThrows(RecintoInvalidoException.class, request::validarDatosRegistro);
    }
}

