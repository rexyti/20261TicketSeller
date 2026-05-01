package com.ticketseller.application.conciliacion;

import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.domain.exception.conciliacion.PagoEnDiscrepanciaException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmarTransaccionUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final PagoRepositoryPort pagoRepositoryPort = mock(PagoRepositoryPort.class);
    private final TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
    private final ConfirmarOcupacionUseCase confirmarOcupacionUseCase = mock(ConfirmarOcupacionUseCase.class);
    private final ConfirmarTransaccionUseCase useCase = new ConfirmarTransaccionUseCase(
            ventaRepositoryPort, pagoRepositoryPort, ticketRepositoryPort, confirmarOcupacionUseCase);

    @Test
    void deberiaConfirmarTransaccionExitosamente() {
        UUID ventaId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        Pago pagoVerificado = pago(pagoId, ventaId, EstadoConciliacion.VERIFICADO);
        Pago pagoConfirmado = pagoVerificado.toBuilder().estado(EstadoConciliacion.CONFIRMADO).build();
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).ventaId(ventaId).asientoId(asientoId).build();
        Asiento ocupado = Asiento.builder().id(asientoId).estado(EstadoAsiento.OCUPADO).build();

        when(pagoRepositoryPort.buscarPorIdExterno("ext-123")).thenReturn(Mono.just(pagoVerificado));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA))
                .thenReturn(Mono.just(Venta.builder().id(ventaId).estado(EstadoVenta.COMPLETADA)
                        .total(BigDecimal.valueOf(100)).fechaCreacion(LocalDateTime.now())
                        .fechaExpiracion(LocalDateTime.now().plusHours(1)).build()));
        when(pagoRepositoryPort.actualizarEstado(pagoId, EstadoConciliacion.CONFIRMADO))
                .thenReturn(Mono.just(pagoConfirmado));
        when(ticketRepositoryPort.buscarPorVenta(ventaId)).thenReturn(Flux.just(ticket));
        when(confirmarOcupacionUseCase.confirmar(asientoId)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(ventaId, "ext-123", BigDecimal.valueOf(100)))
                .assertNext(p -> assertEquals(EstadoConciliacion.CONFIRMADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaConfirmarOcupacionDeAsientosTrasTransaccionExitosa() {
        UUID ventaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        Venta venta = Venta.builder().id(ventaId).estado(EstadoVenta.PENDIENTE)
                .total(BigDecimal.valueOf(50)).fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(1)).build();
        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).ventaId(ventaId).asientoId(asientoId).build();
        Asiento ocupado = Asiento.builder().id(asientoId).estado(EstadoAsiento.OCUPADO).build();
        Pago pagoGuardado = pago(UUID.randomUUID(), ventaId, EstadoConciliacion.CONFIRMADO);

        when(pagoRepositoryPort.buscarPorIdExterno("ext-new")).thenReturn(Mono.empty());
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA)).thenReturn(Mono.just(venta));
        when(pagoRepositoryPort.guardar(any())).thenReturn(Mono.just(pagoGuardado));
        when(ticketRepositoryPort.buscarPorVenta(ventaId)).thenReturn(Flux.just(ticket));
        when(confirmarOcupacionUseCase.confirmar(asientoId)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(useCase.ejecutar(ventaId, "ext-new", BigDecimal.valueOf(50)))
                .assertNext(p -> assertEquals(EstadoConciliacion.CONFIRMADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaRetornarIdempotenteSiYaConfirmado() {
        UUID ventaId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pago pagoYaConfirmado = pago(pagoId, ventaId, EstadoConciliacion.CONFIRMADO);

        when(pagoRepositoryPort.buscarPorIdExterno("ext-dup")).thenReturn(Mono.just(pagoYaConfirmado));

        StepVerifier.create(useCase.ejecutar(ventaId, "ext-dup", BigDecimal.valueOf(100)))
                .assertNext(p -> assertEquals(EstadoConciliacion.CONFIRMADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiPagoEnDiscrepancia() {
        UUID ventaId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pago pagoDiscrepancia = pago(pagoId, ventaId, EstadoConciliacion.EN_DISCREPANCIA);

        when(pagoRepositoryPort.buscarPorIdExterno("ext-disc")).thenReturn(Mono.just(pagoDiscrepancia));

        StepVerifier.create(useCase.ejecutar(ventaId, "ext-disc", BigDecimal.valueOf(100)))
                .expectError(PagoEnDiscrepanciaException.class)
                .verify();
    }

    private Pago pago(UUID pagoId, UUID ventaId, EstadoConciliacion estado) {
        return Pago.builder()
                .id(pagoId)
                .ventaId(ventaId)
                .idExternoPasarela("ext-123")
                .montoEsperado(BigDecimal.valueOf(100))
                .montoPasarela(BigDecimal.valueOf(100))
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }
}
