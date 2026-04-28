package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.domain.model.asiento.TransicionEstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.asiento.CambiarEstadoMasivoResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class CambiarEstadoMasivoUseCase {
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final HistorialCambioEstadoRepositoryPort historialRepositoryPort;

    public Mono<CambiarEstadoMasivoResponse> ejecutar(UUID eventoId, List<UUID> asientoIds, EstadoAsiento estadoDestino,
                                                      String motivo, String usuarioId) {
        List<String> mensajes = new ArrayList<>();
        AtomicInteger modificados = new AtomicInteger(0);
        AtomicInteger omitidos = new AtomicInteger(0);

        return Flux.fromIterable(asientoIds)
                .flatMap(asientoId -> asientoRepositoryPort.buscarPorId(asientoId)
                        .switchIfEmpty(Mono.defer(() -> {
                            mensajes.add(String.format("Asiento %s no encontrado", asientoId));
                            omitidos.incrementAndGet();
                            return Mono.empty();
                        }))
                )
                .flatMap(asiento -> procesarAsiento(asiento, estadoDestino, eventoId, motivo,
                        usuarioId, mensajes, modificados, omitidos))
                .then(Mono.fromCallable(() -> new CambiarEstadoMasivoResponse(modificados.get(),
                        omitidos.get(), mensajes)));
    }

    private Mono<Void> procesarAsiento(Asiento asiento, EstadoAsiento estadoDestino, UUID eventoId,
                                       String motivo, String usuarioId, List<String> mensajes,
                                       AtomicInteger modificados, AtomicInteger omitidos) {
        return Mono.just(asiento)
                .filter(a -> TransicionEstadoAsiento.esPermitida(a.getEstado(), estadoDestino))
                .switchIfEmpty(Mono.defer(() -> {
                    mensajes.add(String.format("Asiento %s en estado %s no puede ser modificado a %s",
                            asiento.getId(), asiento.getEstado(), estadoDestino));
                    omitidos.incrementAndGet();
                    return Mono.empty();
                }))
                .flatMap(a -> {
                    EstadoAsiento estadoAnterior = a.getEstado();
                    Asiento actualizado = a.toBuilder().estado(estadoDestino).build();
                    return asientoRepositoryPort.guardar(actualizado)
                            .flatMap(guardado -> guardarHistorial(eventoId, guardado, estadoAnterior, motivo, usuarioId))
                            .doOnSuccess(ignored -> modificados.incrementAndGet());
                })
                .then();
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
