package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
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
                .flatMap(id -> asientoRepositoryPort.buscarPorId(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                "Asiento %s no encontrado".formatted(id)))))
                .doOnNext(this::validarDisponible)
                .collectList()
                .flatMap(asientos -> bloquearTodos(asientos, eventoId, destinatario, fechaExpiracion));
    }

    private void validarDisponible(Asiento asiento) {
        if (EstadoAsiento.BLOQUEADO.equals(asiento.getEstado())) {
            throw new AsientoYaBloqueadoException(asiento.getId());
        }
        if (!EstadoAsiento.DISPONIBLE.equals(asiento.getEstado())) {
            throw new AsientoOcupadoException(asiento.getId());
        }
    }

    private Mono<List<Bloqueo>> bloquearTodos(List<Asiento> asientos, UUID eventoId,
                                               String destinatario, LocalDateTime fechaExpiracion) {
        List<Asiento> bloqueados = asientos.stream()
                .map(a -> a.toBuilder().estado(EstadoAsiento.BLOQUEADO).build())
                .toList();
        return asientoRepositoryPort.guardarTodos(bloqueados)
                .flatMap(asiento -> crearBloqueo(asiento.getId(), eventoId, destinatario, fechaExpiracion))
                .collectList();
    }

    private Mono<Bloqueo> crearBloqueo(UUID asientoId, UUID eventoId,
                                        String destinatario, LocalDateTime fechaExpiracion) {
        Bloqueo bloqueo = Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario(destinatario)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(fechaExpiracion)
                .estado(EstadoBloqueo.ACTIVO)
                .build();
        bloqueo.validar();
        return bloqueoRepositoryPort.guardar(bloqueo);
    }
}
