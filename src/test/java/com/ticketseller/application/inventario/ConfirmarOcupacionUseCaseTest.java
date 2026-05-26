package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
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

class ConfirmarOcupacionUseCaseTest {

    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
    private final ConfirmarOcupacionUseCase useCase = new ConfirmarOcupacionUseCase(asientoHoldRepositoryPort);
    private final UUID eventoId = UUID.randomUUID();

    @Test
    void confirmaOcupacionExitosaCuandoHoldVigente() {
        UUID id = UUID.randomUUID();
        AsientoHold hold = AsientoHold.builder()
                .asientoId(id)
                .eventoId(eventoId)
                .expiraEn(LocalDateTime.now().plusMinutes(10))
                .estado(EstadoHold.RESERVADO)
                .build();
        AsientoHold vendido = hold.toBuilder().estado(EstadoHold.VENDIDO).build();

        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.just(hold));
        when(asientoHoldRepositoryPort.guardar(any())).thenReturn(Mono.just(vendido));

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .assertNext(h -> assertEquals(EstadoHold.VENDIDO, h.getEstado()))
                .verifyComplete();
    }

    @Test
    void fallaConHoldExpiradoCuandoNoExisteHoldActivo() {
        UUID id = UUID.randomUUID();
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .expectError(HoldExpiradoException.class)
                .verify();
    }

    @Test
    void fallaConHoldExpiradoCuandoEventoDiferente() {
        UUID id = UUID.randomUUID();
        UUID otroEventoId = UUID.randomUUID();
        when(asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(id, otroEventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, otroEventoId))
                .expectError(HoldExpiradoException.class)
                .verify();
    }
}
