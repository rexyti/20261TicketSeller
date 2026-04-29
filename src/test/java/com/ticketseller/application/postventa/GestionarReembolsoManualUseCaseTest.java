package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.ReembolsoFallidoException;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestionarReembolsoManualUseCaseTest {

    @Test
    void deberiaReembolsarManualTotal() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        GestionarReembolsoManualUseCase useCase = new GestionarReembolsoManualUseCase(
                ticketRepositoryPort, reembolsoRepositoryPort, pasarelaPagoPort, notificacionEmailPort, ventaRepositoryPort);

        UUID ticketId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(ventaId)
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.valueOf(100))
                .build();
        Venta venta = Venta.builder()
                .id(ventaId)
                .compradorId(UUID.randomUUID())
                .eventoId(ticket.getEventoId())
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .total(BigDecimal.valueOf(100))
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.empty());
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pasarelaPagoPort.procesarReembolso(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(true, "APROBADO", "REF", "OK")));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(notificacionEmailPort.enviarReembolsoCompletado(any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, null, UUID.randomUUID()))
                .expectNextMatches(reembolso -> EstadoReembolso.COMPLETADO.equals(reembolso.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaMarcarFalloSiPasarelaRechaza() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        GestionarReembolsoManualUseCase useCase = new GestionarReembolsoManualUseCase(
                ticketRepositoryPort, reembolsoRepositoryPort, pasarelaPagoPort, notificacionEmailPort, ventaRepositoryPort);

        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.valueOf(100))
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.empty());
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(pasarelaPagoPort.procesarReembolso(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(false, "RECHAZADO", null, "Error")));
        when(notificacionEmailPort.enviarAlertaSoporteReembolsoFallido(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, null, UUID.randomUUID()))
                .expectError(ReembolsoFallidoException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiMontoParcialEsMayorAlOriginal() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        ReembolsoRepositoryPort reembolsoRepositoryPort = mock(ReembolsoRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        GestionarReembolsoManualUseCase useCase = new GestionarReembolsoManualUseCase(
                ticketRepositoryPort, reembolsoRepositoryPort, pasarelaPagoPort, notificacionEmailPort, ventaRepositoryPort);

        UUID ticketId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.valueOf(100))
                .build();

        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.PARCIAL, BigDecimal.valueOf(150), UUID.randomUUID()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}

