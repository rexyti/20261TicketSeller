package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmarOcupacionUseCaseTest {

    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final ConfirmarOcupacionUseCase useCase = new ConfirmarOcupacionUseCase(asientoRepositoryPort);

    @Test
    void confirmaOcupacionExitosaCuandoHoldVigente() {
        UUID id = UUID.randomUUID();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(10)).build();
        Asiento ocupado = Asiento.builder().id(id).estado(EstadoAsiento.OCUPADO).build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(reservado));
        when(asientoRepositoryPort.marcarOcupado(id)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.confirmar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.OCUPADO, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConHoldExpiradoCuandoExpiraEnEsPasado() {
        UUID id = UUID.randomUUID();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO)
                .expiraEn(LocalDateTime.now().minusMinutes(1)).build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(reservado));

        StepVerifier.create(useCase.confirmar(id))
                .expectError(HoldExpiradoException.class)
                .verify();
    }

    @Test
    void fallaConAsientoNoDisponibleCuandoEstadoNoEsReservado() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(disponible));

        StepVerifier.create(useCase.confirmar(id))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }

    @Test
    void liberaHoldExitosamente() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.liberarHold(id)).thenReturn(Mono.just(disponible));

        StepVerifier.create(useCase.liberar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.DISPONIBLE, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConAsientoNoDisponibleAlLiberarAsientoInexistente() {
        UUID id = UUID.randomUUID();
        when(asientoRepositoryPort.liberarHold(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.liberar(id))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
