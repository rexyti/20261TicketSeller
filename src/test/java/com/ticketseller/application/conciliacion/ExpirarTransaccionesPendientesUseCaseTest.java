package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
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

class ExpirarTransaccionesPendientesUseCaseTest {

    private final PagoRepositoryPort pagoRepositoryPort = mock(PagoRepositoryPort.class);
    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final ExpirarTransaccionesPendientesUseCase useCase = new ExpirarTransaccionesPendientesUseCase(pagoRepositoryPort, ventaRepositoryPort);

    @Test
    void deberiaExpirarPagosPendientesAntiguos() {
        UUID ventaId = UUID.randomUUID();
        UUID pagoId = UUID.randomUUID();
        Pago pagoPendiente = Pago.builder()
                .id(pagoId).ventaId(ventaId)
                .montoEsperado(BigDecimal.valueOf(100))
                .estado(EstadoConciliacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now().minusMinutes(20))
                .fechaActualizacion(LocalDateTime.now().minusMinutes(20))
                .build();
        Pago pagoExpirado = pagoPendiente.toBuilder().estado(EstadoConciliacion.EXPIRADO).build();

        when(pagoRepositoryPort.buscarPendientesAnterioresA(any())).thenReturn(Flux.just(pagoPendiente));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.EXPIRADA))
                .thenReturn(Mono.just(Venta.builder().id(ventaId).estado(EstadoVenta.EXPIRADA)
                        .total(BigDecimal.valueOf(100)).fechaCreacion(LocalDateTime.now())
                        .fechaExpiracion(LocalDateTime.now().minusMinutes(15)).build()));
        when(pagoRepositoryPort.actualizarEstado(pagoId, EstadoConciliacion.EXPIRADO))
                .thenReturn(Mono.just(pagoExpirado));

        StepVerifier.create(useCase.ejecutar(LocalDateTime.now().minusMinutes(15)))
                .assertNext(p -> assertEquals(EstadoConciliacion.EXPIRADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaCompletarSinErrorSiNoHayPendientes() {
        when(pagoRepositoryPort.buscarPendientesAnterioresA(any())).thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(LocalDateTime.now().minusMinutes(15)))
                .verifyComplete();
    }
}
