package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiberarBloqueoUseCaseTest {

    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private LiberarBloqueoUseCase useCase;

    private final UUID bloqueoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        useCase = new LiberarBloqueoUseCase(bloqueoRepositoryPort);
    }

    @Test
    void liberarBloqueoActualizaBloqueoALiberado() {
        Bloqueo bloqueo = buildBloqueo(EstadoBloqueo.ACTIVO);

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(bloqueoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(bloqueoId))
                .assertNext(b -> assertEquals(EstadoBloqueo.LIBERADO, b.getEstado()))
                .verifyComplete();

        verify(bloqueoRepositoryPort).guardar(argThat(b -> EstadoBloqueo.LIBERADO.equals(b.getEstado())));
    }

    @Test
    void liberarBloqueoNoEncontradoLanzaExcepcion() {
        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(bloqueoId))
                .expectError(BloqueoNoEncontradoException.class)
                .verify();
    }

    private Bloqueo buildBloqueo(EstadoBloqueo estado) {
        return Bloqueo.builder()
                .id(bloqueoId)
                .asientoId(asientoId)
                .eventoId(UUID.randomUUID())
                .destinatario("Sponsor Original")
                .estado(estado)
                .build();
    }
}
