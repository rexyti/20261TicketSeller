package com.ticketseller.domain;

import com.ticketseller.domain.exception.EventoEnProgresoException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventoTest {

    @Test
    void deberiaValidarRegistroConFechasInvalidas() {
        Evento evento = Evento.builder()
                .nombre("Concierto")
                .tipo("MUSICAL")
                .recintoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().plusDays(2))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .build();

        assertThrows(IllegalArgumentException.class, evento::validarDatosRegistro);
    }

    @Test
    void deberiaBloquearEdicionCuandoEstaEnProgreso() {
        Evento evento = Evento.builder()
                .estado(EstadoEvento.EN_PROGRESO)
                .build();

        assertThrows(EventoEnProgresoException.class, evento::validarEditable);
    }

    @Test
    void deberiaCancelarEvento() {
        Evento evento = Evento.builder()
                .id(UUID.randomUUID())
                .estado(EstadoEvento.ACTIVO)
                .build();

        Evento cancelado = evento.cancelar();

        assertEquals(EstadoEvento.CANCELADO, cancelado.getEstado());
    }
}

