package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEstadoReembolsoUseCaseTest {

    @Mock
    private VentaRepositoryPort ventaRepositoryPort;
    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @InjectMocks
    private ConsultarEstadoReembolsoUseCase useCase;

    private UUID compradorId;
    private UUID ventaId;
    private UUID ticketId;
    private Venta venta;
    private Ticket ticket;
    private Reembolso reembolso;

    @BeforeEach
    void setUp() {
        compradorId = UUID.randomUUID();
        ventaId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        venta = Venta.builder()
                .id(ventaId)
                .compradorId(compradorId)
                .eventoId(UUID.randomUUID())
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .total(BigDecimal.TEN)
                .build();
        ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(ventaId)
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.TEN)
                .build();
        reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.TEN)
                .estado(EstadoReembolso.PENDIENTE)
                .build();
    }

    @Test
    void deberiaConsultarTicketsConEstadoDeReembolso() {
        when(ventaRepositoryPort.buscarPorCompradorId(compradorId)).thenReturn(Flux.just(venta));
        when(ticketRepositoryPort.buscarPorVentaIds(any())).thenReturn(Flux.fromIterable(List.of(ticket)));
        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.just(reembolso));

        StepVerifier.create(useCase.ejecutar(compradorId))
                .expectNextMatches(item -> item.reembolso() != null && EstadoReembolso.PENDIENTE.equals(item.reembolso().getEstado()))
                .verifyComplete();
    }
}
