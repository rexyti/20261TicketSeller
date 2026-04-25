package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcesarReembolsoMasivoUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;

    private ProcesarReembolsoMasivoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarReembolsoMasivoUseCase(ticketRepositoryPort, reembolsoRepositoryPort);
    }

    @Test
    void procesarReembolsoMasivoExitoso() {
        UUID eventoId = UUID.randomUUID();
        Ticket t1 = Ticket.builder().id(UUID.randomUUID()).estado(EstadoTicket.VENDIDO).precio(BigDecimal.valueOf(50)).esCortesia(false).build();
        Ticket t2 = Ticket.builder().id(UUID.randomUUID()).estado(EstadoTicket.VENDIDO).precio(BigDecimal.valueOf(0)).esCortesia(true).build();

        when(ticketRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.just(t1, t2));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(t1));
        when(reembolsoRepositoryPort.save(any())).thenReturn(Mono.just(Reembolso.builder().build()));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .verifyComplete();

        verify(ticketRepositoryPort).guardar(argThat(t -> t.getId().equals(t1.getId()) && t.getEstado() == EstadoTicket.CANCELADO));
        verify(ticketRepositoryPort).guardar(argThat(t -> t.getId().equals(t2.getId()) && t.getEstado() == EstadoTicket.ANULADO));
        verify(reembolsoRepositoryPort, times(1)).save(any()); // Solo para t1
    }
}
