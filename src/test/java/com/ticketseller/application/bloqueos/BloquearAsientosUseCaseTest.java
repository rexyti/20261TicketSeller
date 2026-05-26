package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
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
    private AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private BloquearAsientosUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId1 = UUID.randomUUID();
    private final UUID asientoId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        useCase = new BloquearAsientosUseCase(asientoRepositoryPort, bloqueoRepositoryPort, asientoHoldRepositoryPort);
    }

    @Test
    void asientosDisponiblesSeBloqueaCorrectamente() {
        Asiento asiento1 = buildAsiento(asientoId1, EstadoAsiento.DISPONIBLE);
        Asiento asiento2 = buildAsiento(asientoId2, EstadoAsiento.DISPONIBLE);

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.empty());
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId2, eventoId)).thenReturn(Mono.empty());
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.empty());
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId2, eventoId)).thenReturn(Mono.empty());
        when(bloqueoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Patrocinador A", null))
                .expectNextMatches(bloqueos -> bloqueos.size() == 2
                        && bloqueos.stream().allMatch(b -> EstadoBloqueo.ACTIVO.equals(b.getEstado()))
                        && bloqueos.stream().allMatch(b -> "Patrocinador A".equals(b.getDestinatario())))
                .verifyComplete();
    }

    @Test
    void asientoYaBloqueadoParaEventoLanzaExcepcion() {
        Asiento asiento = buildAsiento(asientoId1, EstadoAsiento.DISPONIBLE);
        Bloqueo bloqueoExistente = buildBloqueo(asientoId1);

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.just(bloqueoExistente));
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), "Sponsor", null))
                .expectError(AsientoYaBloqueadoException.class)
                .verify();

        verify(bloqueoRepositoryPort, never()).guardar(any());
    }

    @Test
    void asientoEnMantenimientoLanzaExcepcion() {
        Asiento enMantenimiento = buildAsiento(asientoId1, EstadoAsiento.MANTENIMIENTO);
        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(enMantenimiento));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), "Sponsor", null))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(bloqueoRepositoryPort, never()).guardar(any());
    }

    @Test
    void listaMixtaConHoldActivoNoBloquea() {
        Asiento disponible = buildAsiento(asientoId1, EstadoAsiento.DISPONIBLE);
        Asiento disponible2 = buildAsiento(asientoId2, EstadoAsiento.DISPONIBLE);
        AsientoHold holdActivo = AsientoHold.builder()
                .asientoId(asientoId2).eventoId(eventoId)
                .estado(EstadoHold.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(disponible2));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.empty());
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asientoId2, eventoId)).thenReturn(Mono.empty());
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId1, eventoId)).thenReturn(Mono.empty());
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asientoId2, eventoId)).thenReturn(Mono.just(holdActivo));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Sponsor", null))
                .expectError(AsientoOcupadoException.class)
                .verify();

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
