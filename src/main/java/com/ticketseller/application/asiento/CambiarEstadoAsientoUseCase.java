package com.ticketseller.application.asiento;

import com.ticketseller.domain.exception.asiento.TransicionEstadoInvalidaException;
import com.ticketseller.domain.exception.asiento.AsientoEnCompraException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.domain.model.asiento.TransicionEstadoAsiento;
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
                .flatMap(asiento -> validarTransicion(asiento, estadoDestino))
                .flatMap(asiento -> validarSinCompraActiva(asientoId).thenReturn(asiento))
                .flatMap(asiento -> aplicarCambioEstado(asiento, estadoDestino, eventoId, motivo, usuarioId));
    }

    private Mono<Asiento> validarTransicion(Asiento asiento, EstadoAsiento estadoDestino) {
        return Mono.just(asiento)
                .filter(a -> TransicionEstadoAsiento.esPermitida(a.getEstado(), estadoDestino))
                .switchIfEmpty(Mono.error(new TransicionEstadoInvalidaException(asiento.getEstado(), estadoDestino)));
    }

    private Mono<Void> validarSinCompraActiva(UUID asientoId) {
        // TODO: integrar con carrito cuando feature 005 esté implementado
        return Mono.just(false)
                .filter(enCompra -> !enCompra)
                .switchIfEmpty(Mono.error(new AsientoEnCompraException("El asiento está siendo reservado por un cliente.")))
                .then();
    }

    private Mono<Asiento> aplicarCambioEstado(Asiento asiento, EstadoAsiento estadoDestino, UUID eventoId, String motivo, String usuarioId) {
        EstadoAsiento estadoAnterior = asiento.getEstado();
        Asiento actualizado = asiento.toBuilder().estado(estadoDestino).build();
        return asientoRepositoryPort.guardar(actualizado)
                .flatMap(guardado -> guardarHistorial(eventoId, guardado, estadoAnterior, motivo, usuarioId).thenReturn(guardado));
    }

    private Mono<HistorialCambioEstado> guardarHistorial(UUID eventoId, Asiento asiento, EstadoAsiento estadoAnterior,
                                                         String motivo, String usuarioId) {
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
