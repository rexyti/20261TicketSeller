package com.ticketseller.application.transaccion;

import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.HistorialEstadoVentaRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHistorialVentaUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final HistorialEstadoVentaRepositoryPort historialRepositoryPort = mock(HistorialEstadoVentaRepositoryPort.class);
    private final ConsultarHistorialVentaUseCase useCase = new ConsultarHistorialVentaUseCase(ventaRepositoryPort, historialRepositoryPort);

    @Test
    void deberiaRetornarHistorialOrdenado() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder().id(ventaId).estado(EstadoVenta.COMPLETADA)
                .total(BigDecimal.TEN).fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(1)).build();

        HistorialEstadoVenta entrada1 = HistorialEstadoVenta.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .estadoAnterior(EstadoVenta.PENDIENTE).estadoNuevo(EstadoVenta.RESERVADA)
                .justificacion("reserva").fechaCambio(LocalDateTime.now().minusMinutes(10)).build();
        HistorialEstadoVenta entrada2 = HistorialEstadoVenta.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .estadoAnterior(EstadoVenta.RESERVADA).estadoNuevo(EstadoVenta.COMPLETADA)
                .justificacion("pago").fechaCambio(LocalDateTime.now()).build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(historialRepositoryPort.buscarPorVentaId(ventaId)).thenReturn(Flux.just(entrada1, entrada2));

        StepVerifier.create(useCase.ejecutar(ventaId))
                .assertNext(h -> assertEquals(EstadoVenta.PENDIENTE, h.getEstadoAnterior()))
                .assertNext(h -> assertEquals(EstadoVenta.COMPLETADA, h.getEstadoNuevo()))
                .verifyComplete();
    }

    @Test
    void deberiaRetornarFluxVacioSiSinHistorial() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder().id(ventaId).estado(EstadoVenta.PENDIENTE)
                .total(BigDecimal.TEN).fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(1)).build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(historialRepositoryPort.buscarPorVentaId(ventaId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(ventaId))
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiVentaNoExiste() {
        UUID ventaId = UUID.randomUUID();
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ventaId))
                .expectError(VentaNoEncontradaException.class)
                .verify();
    }
}
