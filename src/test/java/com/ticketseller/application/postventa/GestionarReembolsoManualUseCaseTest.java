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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarReembolsoManualUseCaseTest {

    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @Mock
    private PasarelaPagoPort pasarelaPagoPort;
    @Mock
    private NotificacionEmailPort notificacionEmailPort;
    @Mock
    private VentaRepositoryPort ventaRepositoryPort;
    @InjectMocks
    private GestionarReembolsoManualUseCase useCase;

    private UUID ticketId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.valueOf(100))
                .build();
        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
    }

    @Test
    void deberiaReembolsarManualTotal() {
        UUID ventaId = ticket.getVentaId();
        Venta venta = Venta.builder()
                .id(ventaId)
                .compradorId(UUID.randomUUID())
                .eventoId(ticket.getEventoId())
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusDays(1))
                .total(BigDecimal.valueOf(100))
                .build();

        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.empty());
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(pasarelaPagoPort.procesarReembolso(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(true, "APROBADO", "REF", "OK")));
        when(ticketRepositoryPort.guardar(any(Ticket.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(notificacionEmailPort.enviarReembolsoCompletado(any(), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, null, UUID.randomUUID()))
                .expectNextMatches(r -> EstadoReembolso.COMPLETADO.equals(r.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaMarcarFalloSiPasarelaRechaza() {
        when(reembolsoRepositoryPort.buscarPorTicketId(ticketId)).thenReturn(Mono.empty());
        when(reembolsoRepositoryPort.guardar(any(Reembolso.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(pasarelaPagoPort.procesarReembolso(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(false, "RECHAZADO", null, "Error")));
        when(notificacionEmailPort.enviarAlertaSoporteReembolsoFallido(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, null, UUID.randomUUID()))
                .expectError(ReembolsoFallidoException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiMontoParcialEsMayorAlOriginal() {
        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.PARCIAL, BigDecimal.valueOf(150), UUID.randomUUID()))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
