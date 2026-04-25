package com.ticketseller.application;

import com.ticketseller.domain.exception.ReembolsoFallidoException;
import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionarReembolsoManualUseCaseTest {

    @Mock
    private ReembolsoRepositoryPort reembolsoRepositoryPort;
    @Mock
    private TicketRepositoryPort ticketRepositoryPort;
    @Mock
    private PasarelaPagoPort pasarelaPagoPort;

    private GestionarReembolsoManualUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GestionarReembolsoManualUseCase(reembolsoRepositoryPort, ticketRepositoryPort, pasarelaPagoPort);
    }

    @Test
    void gestionarReembolsoExitoso() {
        UUID ticketId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        Reembolso reembolso = Reembolso.builder()
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.valueOf(100))
                .estado(EstadoReembolso.PENDIENTE)
                .build();
        Ticket ticket = Ticket.builder().id(ticketId).build();

        when(reembolsoRepositoryPort.findByTicketId(ticketId)).thenReturn(Mono.just(reembolso));
        when(pasarelaPagoPort.procesarReembolso(any(), any())).thenReturn(Mono.just(new ResultadoPago(true, "OK", "AUTH", "Aprobado")));
        when(reembolsoRepositoryPort.save(any())).thenReturn(Mono.just(reembolso));
        when(ticketRepositoryPort.buscarPorId(ticketId)).thenReturn(Mono.just(ticket));
        when(ticketRepositoryPort.guardar(any())).thenReturn(Mono.just(ticket));

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, UUID.randomUUID()))
                .expectNextCount(1)
                .verifyComplete();

        verify(reembolsoRepositoryPort).save(argThat(r -> r.getEstado() == EstadoReembolso.COMPLETADO));
        verify(ticketRepositoryPort).guardar(argThat(t -> t.getEstado() == EstadoTicket.REEMBOLSADO));
    }

    @Test
    void marcarComoFallidoSiPasarelaRechaza() {
        UUID ticketId = UUID.randomUUID();
        Reembolso reembolso = Reembolso.builder().ticketId(ticketId).monto(BigDecimal.valueOf(100)).build();

        when(reembolsoRepositoryPort.findByTicketId(ticketId)).thenReturn(Mono.just(reembolso));
        when(pasarelaPagoPort.procesarReembolso(any(), any())).thenReturn(Mono.just(new ResultadoPago(false, "ERROR", null, "Rechazado")));
        when(reembolsoRepositoryPort.save(any())).thenReturn(Mono.just(reembolso));

        StepVerifier.create(useCase.ejecutar(ticketId, TipoReembolso.TOTAL, UUID.randomUUID()))
                .expectError(ReembolsoFallidoException.class)
                .verify();

        verify(reembolsoRepositoryPort).save(argThat(r -> r.getEstado() == EstadoReembolso.FALLIDO));
    }
}
