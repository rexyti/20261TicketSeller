package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiberarHoldUseCaseTest {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
    private final LiberarHoldUseCase useCase = new LiberarHoldUseCase(asientoHoldRepositoryPort);
    private final UUID eventoId = UUID.randomUUID();

    @Test
    void liberaHoldExitosamente() {
        UUID id = UUID.randomUUID();
        AsientoHold hold = AsientoHold.builder()
                .asientoId(id).eventoId(eventoId)
                .estado(EstadoHold.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .build();
        AsientoHold expirado = hold.toBuilder().estado(EstadoHold.EXPIRADO).build();

        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.just(hold));
        when(asientoHoldRepositoryPort.guardar(any())).thenReturn(Mono.just(expirado));

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .assertNext(h -> assertEquals(EstadoHold.EXPIRADO, h.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConAsientoNoDisponibleAlLiberarHoldInexistente() {
        UUID id = UUID.randomUUID();
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
