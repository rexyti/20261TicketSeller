package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoReembolsoUseCaseTest {

    @Mock
    private VentaRepositoryPort ventaRepositoryPort;
    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;

    private ConsultarEstadoReembolsoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstadoReembolsoUseCase(ventaRepositoryPort, ticketRepositoryPort, reembolsoRepositoryPort);
    }

    @Test
    void consultarMisComprasConReembolsos() {
        UUID compradorId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        
        Venta venta = Venta.builder().id(ventaId).build();
        Ticket ticket = Ticket.builder().id(ticketId).ventaId(ventaId).estado(EstadoTicket.CANCELADO).build();
        Reembolso reembolso = Reembolso.builder().ticketId(ticketId).estado(EstadoReembolso.COMPLETADO).build();

        when(ventaRepositoryPort.buscarPorComprador(compradorId)).thenReturn(Flux.just(venta));
        when(ticketRepositoryPort.buscarPorVenta(ventaId)).thenReturn(Flux.just(ticket));
        when(reembolsoRepositoryPort.findByTicketId(ticketId)).thenReturn(Mono.just(reembolso));

        StepVerifier.create(useCase.ejecutar(compradorId))
                .expectNextMatches(response -> response.id().equals(ticketId) && response.estadoReembolso() == EstadoReembolso.COMPLETADO)
                .verifyComplete();
    }
}
