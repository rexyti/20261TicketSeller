package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BloquearAsientosUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private BloquearAsientosUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId1 = UUID.randomUUID();
    private final UUID asientoId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        useCase = new BloquearAsientosUseCase(asientoRepositoryPort, bloqueoRepositoryPort);
    }

    @Test
    void asientosDisponiblesSeBloqueaCorrectamente() {
        Asiento asiento1 = buildAsiento(asientoId1, EstadoAsiento.DISPONIBLE);
        Asiento asiento2 = buildAsiento(asientoId2, EstadoAsiento.DISPONIBLE);
        Asiento bloqueado1 = asiento1.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();
        Asiento bloqueado2 = asiento2.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));
        when(asientoRepositoryPort.guardarTodos(any())).thenReturn(Flux.just(bloqueado1, bloqueado2));
        when(bloqueoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Patrocinador A", null))
                .expectNextMatches(bloqueos -> bloqueos.size() == 2
                        && bloqueos.stream().allMatch(b -> EstadoBloqueo.ACTIVO.equals(b.getEstado()))
                        && bloqueos.stream().allMatch(b -> "Patrocinador A".equals(b.getDestinatario())))
                .verifyComplete();
    }

    @Test
    void asientoYaBloqueadoLanzaExcepcion() {
        Asiento bloqueado = buildAsiento(asientoId1, EstadoAsiento.BLOQUEADO);
        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(bloqueado));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), "Sponsor", null))
                .expectError(AsientoYaBloqueadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardarTodos(any());
        verify(bloqueoRepositoryPort, never()).guardar(any());
    }

    @Test
    void asientoOcupadoLanzaExcepcion() {
        Asiento ocupado = buildAsiento(asientoId1, EstadoAsiento.OCUPADO);
        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), "Sponsor", null))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardarTodos(any());
        verify(bloqueoRepositoryPort, never()).guardar(any());
    }

    @Test
    void listaMixtaConOcupadoNoBloquea() {
        Asiento disponible = buildAsiento(asientoId1, EstadoAsiento.DISPONIBLE);
        Asiento vendido = buildAsiento(asientoId2, EstadoAsiento.VENDIDO);

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(vendido));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Sponsor", null))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardarTodos(any());
        verify(bloqueoRepositoryPort, never()).guardar(any());
    }

    private Asiento buildAsiento(UUID id, EstadoAsiento estado) {
        return Asiento.builder().id(id).estado(estado).zonaId(UUID.randomUUID()).build();
    }

    private Bloqueo buildBloqueo(UUID asientoId) {
        return Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario("Patrocinador A")
                .estado(EstadoBloqueo.ACTIVO)
                .build();
    }
}
