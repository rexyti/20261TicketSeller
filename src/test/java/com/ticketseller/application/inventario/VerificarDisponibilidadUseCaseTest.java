package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificarDisponibilidadUseCaseTest {

    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final VerificarDisponibilidadUseCase useCase = new VerificarDisponibilidadUseCase(asientoRepositoryPort);

    @Test
    void retornaAsientoDisponible() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> {
                    assertEquals(EstadoAsiento.DISPONIBLE, a.getEstado());
                    assertEquals(id, a.getId());
                })
                .verifyComplete();
    }

    @Test
    void retornaAsientoReservado() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.RESERVADO, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void retornaAsientoOcupado() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.OCUPADO).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.OCUPADO, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void retornaVacioSiAsientoNoExiste() {
        UUID id = UUID.randomUUID();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id))
                .verifyComplete();
    }
}
