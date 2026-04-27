package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoReservadoPorOtroException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservarAsientoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private ReservarAsientoUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new ReservarAsientoUseCase(asientoRepositoryPort);
    }

    @Test
    void reservaExitosaRetornaRespuestaReservado() {
        UUID asientoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        Asiento asientoReservado = Asiento.builder().id(asientoId).build();

        when(asientoRepositoryPort.reservarConHold(eq(asientoId), any(LocalDateTime.class)))
                .thenReturn(Mono.just(asientoReservado));

        StepVerifier.create(useCase.ejecutar(asientoId, ventaId))
                .expectNextMatches(response -> !response.disponible()
                        && "RESERVADO".equals(response.mensaje())
                        && asientoId.equals(response.asientoId()))
                .verifyComplete();
    }

    @Test
    void conflictoOptimistaSeTransformaAExcepcionDominio() {
        UUID asientoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();

        when(asientoRepositoryPort.reservarConHold(eq(asientoId), any(LocalDateTime.class)))
                .thenReturn(Mono.error(new OptimisticLockingFailureException("conflict")));

        StepVerifier.create(useCase.ejecutar(asientoId, ventaId))
                .expectError(AsientoReservadoPorOtroException.class)
                .verify();
    }
}
