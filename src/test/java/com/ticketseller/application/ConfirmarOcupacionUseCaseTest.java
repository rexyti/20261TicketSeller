package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.HoldExpiradoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmarOcupacionUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private ConfirmarOcupacionUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new ConfirmarOcupacionUseCase(asientoRepositoryPort);
    }

    @Test
    void asientoReservadoVigenteSeMarcaComoOcupado() {
        UUID asientoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(5))
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.marcarOcupado(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .verifyComplete();
    }

    @Test
    void asientoNoReservadoRetornaError() {
        UUID asientoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectError(AsientoNoDisponibleException.class)
                .verify();

        verify(asientoRepositoryPort, never()).marcarOcupado(asientoId);
    }

    @Test
    void holdExpiradoRetornaError() {
        UUID asientoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.RESERVADO)
                .expiraEn(LocalDateTime.now().minusMinutes(1))
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(asientoId))
                .expectError(HoldExpiradoException.class)
                .verify();

        verify(asientoRepositoryPort, never()).marcarOcupado(asientoId);
    }
}
