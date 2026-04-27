package com.ticketseller.application;

import com.ticketseller.domain.exception.CortesiaNoEncontradaException;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GestionarCortesiaUseCaseTest {

    private CortesiaRepositoryPort cortesiaRepositoryPort;
    private GestionarCortesiaUseCase useCase;

    @BeforeEach
    void setUp() {
        cortesiaRepositoryPort = mock(CortesiaRepositoryPort.class);
        useCase = new GestionarCortesiaUseCase(cortesiaRepositoryPort);
    }

    @Test
    void editaDestinatarioExitosamente() {
        UUID cortesiaId = UUID.randomUUID();
        Cortesia cortesia = Cortesia.builder().id(cortesiaId).destinatario("Invitado A").build();

        when(cortesiaRepositoryPort.buscarPorId(cortesiaId)).thenReturn(Mono.just(cortesia));
        when(cortesiaRepositoryPort.guardar(any(Cortesia.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.editarDestinatario(cortesiaId, "Invitado B"))
                .expectNextMatches(c -> "Invitado B".equals(c.getDestinatario()))
                .verifyComplete();

        verify(cortesiaRepositoryPort).guardar(any(Cortesia.class));
    }

    @Test
    void fallaSiCortesiaNoExiste() {
        UUID cortesiaId = UUID.randomUUID();

        when(cortesiaRepositoryPort.buscarPorId(cortesiaId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.editarDestinatario(cortesiaId, "Invitado B"))
                .expectError(CortesiaNoEncontradaException.class)
                .verify();

        verify(cortesiaRepositoryPort, never()).guardar(any(Cortesia.class));
    }
}
