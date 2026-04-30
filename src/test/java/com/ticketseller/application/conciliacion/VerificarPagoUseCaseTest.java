package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
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

class VerificarPagoUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final PagoRepositoryPort pagoRepositoryPort = mock(PagoRepositoryPort.class);
    private final VerificarPagoUseCase useCase = new VerificarPagoUseCase(ventaRepositoryPort, pagoRepositoryPort);

    @Test
    void deberiaMarcarsePagoComoVerificadoSiMontoCoincide() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = ventaReservada(ventaId, BigDecimal.valueOf(100));

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(pagoRepositoryPort.buscarPorIdExterno(any())).thenReturn(Mono.empty());
        when(pagoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(ventaId, BigDecimal.valueOf(100), "ext-123"))
                .assertNext(pago -> {
                    assertEquals(EstadoConciliacion.VERIFICADO, pago.getEstado());
                    assertEquals(ventaId, pago.getVentaId());
                })
                .verifyComplete();
    }

    @Test
    void deberiaMarcarsePagoEnDiscrepanciaSiMontoNoConcide() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = ventaReservada(ventaId, BigDecimal.valueOf(100));

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(pagoRepositoryPort.buscarPorIdExterno(any())).thenReturn(Mono.empty());
        when(pagoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(ventaId, BigDecimal.valueOf(50), "ext-456"))
                .assertNext(pago -> assertEquals(EstadoConciliacion.EN_DISCREPANCIA, pago.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaRetornarPagoExistenteIdempotentemente() {
        UUID ventaId = UUID.randomUUID();
        Pago pagoExistente = Pago.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .idExternoPasarela("ext-789")
                .estado(EstadoConciliacion.VERIFICADO)
                .montoEsperado(BigDecimal.valueOf(100))
                .montoPasarela(BigDecimal.valueOf(100))
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(pagoRepositoryPort.buscarPorIdExterno("ext-789")).thenReturn(Mono.just(pagoExistente));

        StepVerifier.create(useCase.ejecutar(ventaId, BigDecimal.valueOf(100), "ext-789"))
                .assertNext(pago -> {
                    assertEquals(EstadoConciliacion.VERIFICADO, pago.getEstado());
                    assertEquals("ext-789", pago.getIdExternoPasarela());
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiVentaNoExiste() {
        UUID ventaId = UUID.randomUUID();
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.empty());
        when(pagoRepositoryPort.buscarPorIdExterno(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ventaId, BigDecimal.valueOf(100), "ext-999"))
                .expectError(VentaNoEncontradaException.class)
                .verify();
    }

    private Venta ventaReservada(UUID id, BigDecimal total) {
        return Venta.builder()
                .id(id).compradorId(UUID.randomUUID()).eventoId(UUID.randomUUID())
                .estado(EstadoVenta.RESERVADA).total(total)
                .fechaCreacion(LocalDateTime.now().minusMinutes(5))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
