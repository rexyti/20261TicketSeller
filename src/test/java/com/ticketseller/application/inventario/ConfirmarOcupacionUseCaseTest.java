package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmarOcupacionUseCaseTest {

    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
    private final ConfirmarOcupacionUseCase useCase = new ConfirmarOcupacionUseCase(asientoRepositoryPort, asientoHoldRepositoryPort);

    @Test
    void confirmaOcupacionExitosaCuandoHoldVigente() {
        UUID id = UUID.randomUUID();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO).build();
        Asiento ocupado = Asiento.builder().id(id).estado(EstadoAsiento.OCUPADO).build();
        AsientoHold hold = AsientoHold.builder()
                .asientoId(id)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .estado(EstadoHold.RESERVADO)
                .build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(reservado));
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoId(id)).thenReturn(Mono.just(hold));
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.OCUPADO, a.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConHoldExpiradoCuandoExpiraEnEsPasado() {
        UUID id = UUID.randomUUID();
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO).build();
        AsientoHold holdExpirado = AsientoHold.builder()
                .asientoId(id)
                .expiraEn(LocalDateTime.now().minusMinutes(1))
                .estado(EstadoHold.RESERVADO)
                .build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(reservado));
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoId(id)).thenReturn(Mono.just(holdExpirado));

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(HoldExpiradoException.class)
                .verify();
    }

    @Test
    void fallaConAsientoNoDisponibleCuandoEstadoNoEsReservado() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(disponible));

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
