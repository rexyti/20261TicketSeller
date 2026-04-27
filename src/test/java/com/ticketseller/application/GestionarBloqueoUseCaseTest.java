package com.ticketseller.application;

import com.ticketseller.domain.exception.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionarBloqueoUseCaseTest {

    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private AsientoRepositoryPort asientoRepositoryPort;
    private GestionarBloqueoUseCase useCase;

    @BeforeEach
    void setUp() {
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new GestionarBloqueoUseCase(bloqueoRepositoryPort, asientoRepositoryPort);
    }

    @Test
    void editaDestinatarioExitosamente() {
        UUID bloqueoId = UUID.randomUUID();
        Bloqueo bloqueo = Bloqueo.builder().id(bloqueoId).destinatario("Sponsor A").build();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(bloqueoRepositoryPort.guardar(any(Bloqueo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.editarDestinatario(bloqueoId, "Sponsor B"))
                .expectNextMatches(b -> "Sponsor B".equals(b.getDestinatario()))
                .verifyComplete();

        verify(bloqueoRepositoryPort).guardar(any(Bloqueo.class));
        verify(asientoRepositoryPort, never()).guardar(any());
    }

    @Test
    void liberarBloqueoActualizaEstadoYLiberaAsiento() {
        UUID bloqueoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        Bloqueo bloqueo = Bloqueo.builder().id(bloqueoId).asientoId(asientoId).estado(EstadoBloqueo.ACTIVO).build();
        Asiento asiento = Asiento.builder().id(asientoId).estado(EstadoAsiento.BLOQUEADO).build();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(bloqueoRepositoryPort.guardar(any(Bloqueo.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.liberarBloqueo(bloqueoId))
                .verifyComplete();

        verify(bloqueoRepositoryPort).guardar(argThat(b -> b.getEstado() == EstadoBloqueo.LIBERADO));
        verify(asientoRepositoryPort).guardar(argThat(a -> a.getEstado() == EstadoAsiento.DISPONIBLE));
    }

    @Test
    void fallaSiBloqueoNoExiste() {
        UUID bloqueoId = UUID.randomUUID();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.editarDestinatario(bloqueoId, "Sponsor B"))
                .expectError(BloqueoNoEncontradoException.class)
                .verify();
    }
}
