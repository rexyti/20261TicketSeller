package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.conciliacion.PagoEnDiscrepanciaException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmarTransaccionUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final PagoRepositoryPort pagoRepositoryPort = mock(PagoRepositoryPort.class);
    private final ConfirmarTransaccionUseCase useCase = new ConfirmarTransaccionUseCase(ventaRepositoryPort, pagoRepositoryPort);

    @Test
    void deberiaConfirmarTransaccionExitosamente() {
        UUID ventaId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pago pagoVerificado = pago(pagoId, ventaId, EstadoConciliacion.VERIFICADO);
        Pago pagoConfirmado = pagoVerificado.toBuilder().estado(EstadoConciliacion.CONFIRMADO).build();

        when(pagoRepositoryPort.buscarPorIdExterno("ext-123")).thenReturn(Mono.just(pagoVerificado));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA))
                .thenReturn(Mono.just(Venta.builder().id(ventaId).estado(EstadoVenta.COMPLETADA)
                        .total(BigDecimal.valueOf(100)).fechaCreacion(LocalDateTime.now())
                        .fechaExpiracion(LocalDateTime.now().plusHours(1)).build()));
        when(pagoRepositoryPort.actualizarEstado(pagoId, EstadoConciliacion.CONFIRMADO))
                .thenReturn(Mono.just(pagoConfirmado));

        StepVerifier.create(useCase.ejecutar(ventaId, "ext-123", BigDecimal.valueOf(100)))
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
