package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.exception.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class BloquearAsientosUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private BloqueoRepositoryPort bloqueoRepositoryPort;
    private BloquearAsientosUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        useCase = new BloquearAsientosUseCase(asientoRepositoryPort, bloqueoRepositoryPort);
    }

    @Test
    void bloqueaAsientosExitosamente() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();
        
        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento asiento2 = Asiento.builder().id(asientoId2).estado(EstadoAsiento.DISPONIBLE).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));
        
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(bloqueoRepositoryPort.guardarTodos(anyList())).thenAnswer(inv -> Flux.fromIterable((List<Bloqueo>) inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Sponsor A", null))
                .expectNextMatches(bloqueos -> bloqueos.size() == 2 &&
                        bloqueos.stream().allMatch(b -> b.getEstado() == EstadoBloqueo.ACTIVO &&
                                "Sponsor A".equals(b.getDestinatario())))
                .verifyComplete();

        verify(asientoRepositoryPort, times(2)).guardar(any(Asiento.class));
        verify(bloqueoRepositoryPort, times(1)).guardarTodos(anyList());
    }

    @Test
    void rechazaSiAlgunAsientoEstaBloqueado() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();

        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento asiento2 = Asiento.builder().id(asientoId2).estado(EstadoAsiento.BLOQUEADO).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), "Sponsor A", null))
                .expectError(AsientoYaBloqueadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardar(any(Asiento.class));
        verify(bloqueoRepositoryPort, never()).guardarTodos(anyList());
    }

    @Test
    void rechazaSiAlgunAsientoEstaOcupado() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        
        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.VENDIDO).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), "Sponsor A", null))
                .expectError(AsientoOcupadoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardar(any(Asiento.class));
    }
}
