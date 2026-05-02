package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionarBloqueoUseCaseTest {

    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private AsientoRepositoryPort asientoRepositoryPort;
    private GestionarBloqueoUseCase useCase;

    private final UUID bloqueoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new GestionarBloqueoUseCase(bloqueoRepositoryPort, asientoRepositoryPort);
    }

    @Test
    void editarDestinatarioCambiaNombreSinMoverAsiento() {
        Bloqueo bloqueo = buildBloqueo(EstadoBloqueo.ACTIVO);
        Bloqueo actualizado = bloqueo.toBuilder().destinatario("Nuevo Sponsor").build();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(bloqueoRepositoryPort.guardar(any())).thenReturn(Mono.just(actualizado));

        StepVerifier.create(useCase.editarDestinatario(bloqueoId, "Nuevo Sponsor"))
                .expectNextMatches(b -> "Nuevo Sponsor".equals(b.getDestinatario())
                        && EstadoBloqueo.ACTIVO.equals(b.getEstado()))
                .verifyComplete();

        verify(asientoRepositoryPort, never()).guardar(any());
    }

    @Test
    void editarDestinatarioNoEncontradoLanzaExcepcion() {
        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.editarDestinatario(bloqueoId, "Sponsor"))
                .expectError(BloqueoNoEncontradoException.class)
                .verify();
    }

    @Test
    void liberarBloqueoActualizaAsientoADisponibleYBloqueoALiberado() {
        Bloqueo bloqueo = buildBloqueo(EstadoBloqueo.ACTIVO);
        Asiento asiento = Asiento.builder().id(asientoId).estado(EstadoAsiento.BLOQUEADO).zonaId(UUID.randomUUID()).build();
        Asiento disponible = asiento.toBuilder().estado(EstadoAsiento.DISPONIBLE).build();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(disponible));
        when(bloqueoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.liberarBloqueo(bloqueoId))
                .verifyComplete();

        verify(asientoRepositoryPort).guardar(argThat(a -> EstadoAsiento.DISPONIBLE.equals(a.getEstado())));
        verify(bloqueoRepositoryPort).guardar(argThat(b -> EstadoBloqueo.LIBERADO.equals(b.getEstado())));
    }

    @Test
    void liberarBloqueoNoEncontradoLanzaExcepcion() {
        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.liberarBloqueo(bloqueoId))
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
