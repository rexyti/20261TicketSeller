package com.ticketseller.application.asiento;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsignarAsientosAZonaUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private ZonaRepositoryPort zonaRepositoryPort;
    private AsignarAsientosAZonaUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        useCase = new AsignarAsientosAZonaUseCase(asientoRepositoryPort, zonaRepositoryPort);
    }

    @Test
    void asignarAsientosExistentesAZonaValida() {
        UUID zonaId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();

        Zona zona = Zona.builder().id(zonaId).nombre("VIP").capacidad(50).build();
        Asiento a1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento a2 = Asiento.builder().id(asientoId2).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento a1Asignado = a1.toBuilder().zonaId(zonaId).build();
        Asiento a2Asignado = a2.toBuilder().zonaId(zonaId).build();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(a1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(a2));
        when(asientoRepositoryPort.guardarTodos(any())).thenReturn(Flux.just(a1Asignado, a2Asignado));

        StepVerifier.create(useCase.ejecutar(List.of(asientoId1, asientoId2), zonaId))
                .expectNextMatches(a -> zonaId.equals(a.getZonaId()))
                .expectNextMatches(a -> zonaId.equals(a.getZonaId()))
                .verifyComplete();
    }

    @Test
    void asignarAsientoActualizaZonaId() {
        UUID zonaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        Zona zona = Zona.builder().id(zonaId).nombre("General").capacidad(100).build();
        Asiento asiento = Asiento.builder().id(asientoId).estado(EstadoAsiento.DISPONIBLE).zonaId(null).build();
        Asiento asientoAsignado = asiento.toBuilder().zonaId(zonaId).build();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardarTodos(any())).thenAnswer(inv -> {
            List<Asiento> guardados = inv.getArgument(0);
            assertEquals(zonaId, guardados.get(0).getZonaId());
            return Flux.just(asientoAsignado);
        });

        StepVerifier.create(useCase.ejecutar(List.of(asientoId), zonaId))
                .expectNextMatches(a -> zonaId.equals(a.getZonaId()))
                .verifyComplete();
    }

    @Test
    void zonaNoEncontradaLanzaZonaNotFoundException() {
        UUID zonaId = UUID.randomUUID();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(List.of(UUID.randomUUID()), zonaId))
                .expectError(ZonaNotFoundException.class)
                .verify();

        verify(asientoRepositoryPort, never()).buscarPorId(any());
        verify(asientoRepositoryPort, never()).guardarTodos(any());
    }

    @Test
    void asientoNoEncontradoLanzaAsientoNotFoundException() {
        UUID zonaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        Zona zona = Zona.builder().id(zonaId).nombre("Palco").capacidad(20).build();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(zona));
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(List.of(asientoId), zonaId))
                .expectError(AsientoNotFoundException.class)
                .verify();

        verify(asientoRepositoryPort, never()).guardarTodos(any());
    }
}
