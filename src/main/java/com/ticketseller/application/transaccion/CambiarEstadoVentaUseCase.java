package com.ticketseller.application.transaccion;

import com.ticketseller.domain.exception.transaccion.TransicionVentaInvalidaException;
import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.HistorialEstadoVentaRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoVentaUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final HistorialEstadoVentaRepositoryPort historialRepositoryPort;

    public Mono<Venta> ejecutar(UUID ventaId, EstadoVenta nuevoEstado, String justificacion, UUID actorId) {
        if (esNuevoEstadoInvalido(nuevoEstado)) {
            return Mono.error(new IllegalArgumentException("nuevoEstado es obligatorio"));
        }
        if (justificacionInvalida(justificacion)) {
            return Mono.error(new IllegalArgumentException("justificacion es obligatoria"));
        }
        String justificacionNormalizada = justificacion.trim();

        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNoEncontradaException(ventaId)))
                .flatMap(venta -> {
                    venta.validarTransicionA(nuevoEstado);
                    return persistirCambio(venta, nuevoEstado, justificacionNormalizada, actorId);
                });
    }

    private boolean esNuevoEstadoInvalido(EstadoVenta nuevoEstado) {
        return nuevoEstado == null;
    }

    private boolean justificacionInvalida(String justificacion) {
        return justificacion == null || justificacion.isBlank();
    }

    private Mono<Venta> persistirCambio(Venta venta, EstadoVenta nuevoEstado, String justificacion, UUID actorId) {
        EstadoVenta estadoAnterior = venta.getEstado();
        return ventaRepositoryPort.actualizarEstadoCondicional(venta.getId(), estadoAnterior, nuevoEstado)
                .switchIfEmpty(Mono.error(new TransicionVentaInvalidaException(
                        "Estado de la venta modificado concurrentemente. Intente nuevamente.")))
                .flatMap(ventaActualizada ->
                        registrarHistorial(ventaActualizada, estadoAnterior, nuevoEstado, justificacion, actorId));
    }

    private Mono<Venta> registrarHistorial(Venta venta, EstadoVenta estadoAnterior, EstadoVenta estadoNuevo,
                                           String justificacion, UUID actorId) {
        HistorialEstadoVenta historial = crearRegistroHistorial(venta, estadoAnterior, estadoNuevo, justificacion, actorId);
        return historialRepositoryPort.guardar(historial).thenReturn(venta);
    }

    private HistorialEstadoVenta crearRegistroHistorial(Venta venta, EstadoVenta estadoAnterior, EstadoVenta estadoNuevo,
                                                        String justificacion, UUID actorId) {
        return HistorialEstadoVenta.builder()
                .id(UUID.randomUUID())
                .ventaId(venta.getId())
                .actorId(actorId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .justificacion(justificacion)
                .fechaCambio(LocalDateTime.now())
                .build();
    }
}
