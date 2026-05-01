package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.AsientoReservadoPorOtroException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservarAsientoUseCaseTest {

    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final ReservarAsientoUseCase useCase = new ReservarAsientoUseCase(asientoRepositoryPort);

    @Test
    void reservaExitosaCuandoAsientoDisponible() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(15)).build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.reservarConHold(eq(id), any(LocalDateTime.class)))
                .thenReturn(Mono.just(reservado));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> {
                    assertEquals(EstadoAsiento.RESERVADO, a.getEstado());
                    assertNotNull(a.getExpiraEn());
                })
                .verifyComplete();
    }

    @Test
    void fallaConAsientoReservadoPorOtroCuandoAsientoNoDisponible() {
        UUID id = UUID.randomUUID();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(reservado));

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(AsientoReservadoPorOtroException.class)
                .verify();
    }

    @Test
    void fallaConAsientoNoDisponibleCuandoAsientoNoExiste() {
        UUID id = UUID.randomUUID();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }

    @Test
    void transformaOptimisticLockEnAsientoReservadoPorOtro() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(disponible));
        when(asientoRepositoryPort.reservarConHold(eq(id), any(LocalDateTime.class)))
                .thenReturn(Mono.error(new OptimisticLockingFailureException("conflicto de version")));

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(AsientoReservadoPorOtroException.class)
                .verify();
    }
}
