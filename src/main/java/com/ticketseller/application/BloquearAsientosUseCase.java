package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.exception.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class BloquearAsientosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final BloqueoRepositoryPort bloqueoRepositoryPort;

    public Mono<List<Bloqueo>> ejecutar(UUID eventoId, List<UUID> asientoIds,
                                         String destinatario, LocalDateTime fechaExpiracion) {
        return Flux.fromIterable(asientoIds)
                .flatMap(asientoId -> asientoRepositoryPort.buscarPorId(asientoId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "Asiento no encontrado: " + asientoId)))
                )
                .collectList()
                .flatMap(asientos -> {
                    validarTodosDisponibles(asientos);
                    return bloquearTodos(eventoId, asientos, destinatario, fechaExpiracion);
                });
    }

    private void validarTodosDisponibles(List<Asiento> asientos) {
        for (Asiento asiento : asientos) {
            if (EstadoAsiento.BLOQUEADO.equals(asiento.getEstado())) {
                throw new AsientoYaBloqueadoException(
                        "El asiento %s ya está bloqueado".formatted(asiento.getId()));
            }
            if (!EstadoAsiento.DISPONIBLE.equals(asiento.getEstado())) {
                throw new AsientoOcupadoException(
                        "El asiento %s no está disponible (estado: %s)".formatted(
                                asiento.getId(), asiento.getEstado()));
            }
        }
    }

    private Mono<List<Bloqueo>> bloquearTodos(UUID eventoId, List<Asiento> asientos,
                                               String destinatario, LocalDateTime fechaExpiracion) {
        return Flux.fromIterable(asientos)
                .flatMap(asiento -> {
                    Asiento bloqueado = asiento.toBuilder()
                            .estado(EstadoAsiento.BLOQUEADO)
                            .build();
                    return asientoRepositoryPort.guardar(bloqueado);
                })
                .collectList()
                .flatMap(asientosBloqueados -> {
                    List<Bloqueo> bloqueos = asientosBloqueados.stream()
                            .map(a -> Bloqueo.builder()
                                    .id(UUID.randomUUID())
                                    .asientoId(a.getId())
                                    .eventoId(eventoId)
                                    .destinatario(destinatario)
                                    .fechaCreacion(LocalDateTime.now())
                                    .fechaExpiracion(fechaExpiracion)
                                    .estado(EstadoBloqueo.ACTIVO)
                                    .build())
                            .toList();
                    return bloqueoRepositoryPort.guardarTodos(bloqueos).collectList();
                });
    }
}
