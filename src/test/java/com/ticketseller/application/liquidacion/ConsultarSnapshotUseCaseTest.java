package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarSnapshotUseCaseTest {

    private final EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
    private final LiquidacionQueryPort liquidacionQueryPort = mock(LiquidacionQueryPort.class);
    private final ConsultarSnapshotUseCase useCase = new ConsultarSnapshotUseCase(eventoRepositoryPort, liquidacionQueryPort);

    @Test
    void deberiaRetornarSnapshotCuandoEventoFinalizado() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = Evento.builder().id(eventoId).estado(EstadoEvento.FINALIZADO).build();
        SnapshotLiquidacion snapshot = SnapshotLiquidacion.builder()
                .eventoId(eventoId)
                .condiciones(Map.of(
                        "VENDIDO_SIN_ASISTENCIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("VENDIDO_SIN_ASISTENCIA").cantidad(10).valorTotal(BigDecimal.valueOf(500000)).build(),
                        "CORTESIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("CORTESIA").cantidad(5).valorTotal(BigDecimal.ZERO).build()
                ))
                .timestampGeneracion(LocalDateTime.now())
                .build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(liquidacionQueryPort.obtenerSnapshotPorEvento(eventoId)).thenReturn(Mono.just(snapshot));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .assertNext(result -> {
                    assert result.getEventoId().equals(eventoId);
                    assert result.getCondiciones().size() == 2;
                    assert result.getCondiciones().get("VENDIDO_SIN_ASISTENCIA").getCantidad() == 10;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoEventoNoFinalizado() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = Evento.builder().id(eventoId).estado(EstadoEvento.ACTIVO).build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .expectError(EventoNoFinalizadoException.class)
                .verify();

        verify(liquidacionQueryPort, never()).obtenerSnapshotPorEvento(eventoId);
    }

    @Test
    void deberiaFallarCuandoEventoEnProgreso() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = Evento.builder().id(eventoId).estado(EstadoEvento.EN_PROGRESO).build();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .expectError(EventoNoFinalizadoException.class)
                .verify();
    }

    @Test
    void deberiaFallarCuandoEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId))
                .expectError(EventoNotFoundException.class)
                .verify();
    }
}
