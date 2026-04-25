package com.ticketseller.application;

import com.ticketseller.domain.exception.TransicionEstadoInvalidaException;
import com.ticketseller.domain.exception.AsientoEnCompraException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.model.TransicionEstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoAsientoUseCase {
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final HistorialCambioEstadoRepositoryPort historialRepositoryPort;

    public Mono<Asiento> ejecutar(UUID eventoId, UUID asientoId, EstadoAsiento estadoDestino, String motivo, String usuarioId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Asiento no encontrado")))
                .flatMap(asiento -> {
                    if (!TransicionEstadoAsiento.esPermitida(asiento.getEstado(), estadoDestino)) {
                        return Mono.error(new TransicionEstadoInvalidaException(asiento.getEstado(), estadoDestino));
                    }
                    return verificarCompraActiva(asientoId)
                            .flatMap(enCompra -> {
                                if (enCompra) {
                                    return Mono.error(new AsientoEnCompraException("El asiento está siendo reservado por un cliente."));
                                }
                                EstadoAsiento estadoAnterior = asiento.getEstado();
                                Asiento asientoActualizado = asiento.toBuilder()
                                        .estado(estadoDestino)
                                        .build();

                                return asientoRepositoryPort.guardar(asientoActualizado)
                                        .flatMap(guardado -> guardarHistorial(eventoId, guardado, estadoAnterior, motivo, usuarioId)
                                                .thenReturn(guardado));
                            });
                });
    }

    private Mono<Boolean> verificarCompraActiva(UUID asientoId) {
        // TODO: integrar con carrito cuando feature 005 esté implementado
        return Mono.just(false);
    }

    private Mono<HistorialCambioEstado> guardarHistorial(UUID eventoId, Asiento asiento, EstadoAsiento estadoAnterior, String motivo, String usuarioId) {
        HistorialCambioEstado historial = HistorialCambioEstado.builder()
                .id(UUID.randomUUID())
                .asientoId(asiento.getId())
                .eventoId(eventoId)
                .usuarioId(usuarioId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(asiento.getEstado())
                .fechaHora(Instant.now())
                .motivo(motivo)
                .build();
        return historialRepositoryPort.guardar(historial);
    }
}
