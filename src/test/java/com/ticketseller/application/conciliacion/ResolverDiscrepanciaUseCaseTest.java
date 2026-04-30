package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.conciliacion.TransaccionNoConfirmadaException;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResolverDiscrepanciaUseCaseTest {

    private final PagoRepositoryPort pagoRepositoryPort = mock(PagoRepositoryPort.class);
    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final ResolverDiscrepanciaUseCase useCase = new ResolverDiscrepanciaUseCase(pagoRepositoryPort, ventaRepositoryPort);

    @Test
    void deberiaConfirmarManualmentePagoEnDiscrepancia() {
        UUID pagoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID agenteId = UUID.randomUUID();
        Pago pago = pagoDiscrepancia(pagoId, ventaId);
        Pago pagoResuelto = pago.toBuilder().estado(EstadoConciliacion.CONFIRMADO_MANUALMENTE).build();

        when(pagoRepositoryPort.buscarPorId(pagoId)).thenReturn(Mono.just(pago));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA))
                .thenReturn(Mono.just(venta(ventaId, EstadoVenta.COMPLETADA)));
        when(pagoRepositoryPort.actualizarConResolucion(eq(pagoId), eq(EstadoConciliacion.CONFIRMADO_MANUALMENTE), any(), any()))
                .thenReturn(Mono.just(pagoResuelto));

        StepVerifier.create(useCase.ejecutar(pagoId, true, agenteId, "Confirmado por soporte"))
                .assertNext(p -> assertEquals(EstadoConciliacion.CONFIRMADO_MANUALMENTE, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaRechazarYExpirarPagoEnDiscrepancia() {
        UUID pagoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID agenteId = UUID.randomUUID();
        Pago pago = pagoDiscrepancia(pagoId, ventaId);
        Pago pagoExpirado = pago.toBuilder().estado(EstadoConciliacion.EXPIRADO).build();

        when(pagoRepositoryPort.buscarPorId(pagoId)).thenReturn(Mono.just(pago));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.FALLIDA))
                .thenReturn(Mono.just(venta(ventaId, EstadoVenta.FALLIDA)));
        when(pagoRepositoryPort.actualizarConResolucion(eq(pagoId), eq(EstadoConciliacion.EXPIRADO), any(), any()))
                .thenReturn(Mono.just(pagoExpirado));

        StepVerifier.create(useCase.ejecutar(pagoId, false, agenteId, "Rechazado por soporte"))
                .assertNext(p -> assertEquals(EstadoConciliacion.EXPIRADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiPagoNoExiste() {
        UUID pagoId = UUID.randomUUID();
        when(pagoRepositoryPort.buscarPorId(pagoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(pagoId, true, UUID.randomUUID(), "Justificacion"))
                .expectError(TransaccionNoConfirmadaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiJustificacionEsBlanca() {
        UUID pagoId = UUID.randomUUID();
        StepVerifier.create(useCase.ejecutar(pagoId, true, UUID.randomUUID(), ""))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private Pago pagoDiscrepancia(UUID id, UUID ventaId) {
        return Pago.builder()
                .id(id).ventaId(ventaId)
                .montoEsperado(BigDecimal.valueOf(100))
                .montoPasarela(BigDecimal.valueOf(80))
                .estado(EstadoConciliacion.EN_DISCREPANCIA)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    private Venta venta(UUID id, EstadoVenta estado) {
        return Venta.builder().id(id).estado(estado)
                .total(BigDecimal.valueOf(100))
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(1))
                .build();
    }
}
