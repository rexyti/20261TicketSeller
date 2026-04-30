package com.ticketseller.application.transaccion;

import com.ticketseller.domain.exception.transaccion.TransicionVentaInvalidaException;
import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.HistorialEstadoVentaRepositoryPort;
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

class CambiarEstadoVentaUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final HistorialEstadoVentaRepositoryPort historialRepositoryPort = mock(HistorialEstadoVentaRepositoryPort.class);
    private final CambiarEstadoVentaUseCase useCase = new CambiarEstadoVentaUseCase(ventaRepositoryPort, historialRepositoryPort);

    @Test
    void deberiaCompletarCambioDeEstadoValido() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = ventaReservada(ventaId);
        Venta ventaCompletada = venta.toBuilder().estado(EstadoVenta.COMPLETADA).build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(ventaRepositoryPort.actualizarEstadoCondicional(ventaId, EstadoVenta.RESERVADA, EstadoVenta.COMPLETADA))
                .thenReturn(Mono.just(ventaCompletada));
        when(historialRepositoryPort.guardar(any(HistorialEstadoVenta.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(ventaId, EstadoVenta.COMPLETADA, "Pago confirmado", UUID.randomUUID()))
                .assertNext(v -> assertEquals(EstadoVenta.COMPLETADA, v.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaRechazarTransicionInvalidaDesdeFallida() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = ventaConEstado(ventaId, EstadoVenta.FALLIDA);

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));

        StepVerifier.create(useCase.ejecutar(ventaId, EstadoVenta.COMPLETADA, "Intentar completar fallida", null))
                .expectError(TransicionVentaInvalidaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiVentaNoExiste() {
        UUID ventaId = UUID.randomUUID();
        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ventaId, EstadoVenta.COMPLETADA, "No existe", null))
                .expectError(VentaNoEncontradaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiJustificacionEsBlanca() {
        UUID ventaId = UUID.randomUUID();
        StepVerifier.create(useCase.ejecutar(ventaId, EstadoVenta.COMPLETADA, "  ", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiNuevoEstadoEsNulo() {
        UUID ventaId = UUID.randomUUID();
        StepVerifier.create(useCase.ejecutar(ventaId, null, "Sin estado", null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deberiaLanzarErrorSiModificacionConcurrente() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = ventaReservada(ventaId);

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(ventaRepositoryPort.actualizarEstadoCondicional(eq(ventaId), any(), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ventaId, EstadoVenta.COMPLETADA, "Test concurrencia", null))
                .expectError(TransicionVentaInvalidaException.class)
                .verify();
    }

    private Venta ventaReservada(UUID id) {
        return ventaConEstado(id, EstadoVenta.RESERVADA);
    }

    private Venta ventaConEstado(UUID id, EstadoVenta estado) {
        return Venta.builder()
                .id(id).compradorId(UUID.randomUUID()).eventoId(UUID.randomUUID())
                .estado(estado)
                .fechaCreacion(LocalDateTime.now().minusMinutes(10))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .total(BigDecimal.valueOf(100))
                .build();
    }
}
