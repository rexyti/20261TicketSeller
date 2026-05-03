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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditarDestinatarioBloqueoUseCaseTest {

    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private EditarDestinatarioBloqueoUseCase useCase;

    private final UUID bloqueoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        useCase = new EditarDestinatarioBloqueoUseCase(bloqueoRepositoryPort);
    }

    @Test
    void editarDestinatarioCambiaNombreSinMoverAsiento() {
        Bloqueo bloqueo = buildBloqueo(EstadoBloqueo.ACTIVO);
        Bloqueo actualizado = bloqueo.toBuilder().destinatario("Nuevo Sponsor").build();

        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.just(bloqueo));
        when(bloqueoRepositoryPort.guardar(any())).thenReturn(Mono.just(actualizado));

        StepVerifier.create(useCase.ejecutar(bloqueoId, "Nuevo Sponsor"))
                .expectNextMatches(b -> "Nuevo Sponsor".equals(b.getDestinatario())
                        && EstadoBloqueo.ACTIVO.equals(b.getEstado()))
                .verifyComplete();
    }

    @Test
    void editarDestinatarioNoEncontradoLanzaExcepcion() {
        when(bloqueoRepositoryPort.buscarPorId(bloqueoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(bloqueoId, "Sponsor"))
                .expectError(BloqueoNoEncontradoException.class)
                .verify();

        verify(bloqueoRepositoryPort, never()).guardar(any());
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
