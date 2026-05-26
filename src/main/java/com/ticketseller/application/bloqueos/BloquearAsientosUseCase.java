package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
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
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;

    public Mono<List<Bloqueo>> ejecutar(UUID eventoId, List<UUID> asientoIds,
                                        String destinatario, LocalDateTime fechaExpiracion) {
        return Flux.fromIterable(asientoIds)
                .flatMap(id -> asientoRepositoryPort.buscarPorId(id)
                        .switchIfEmpty(Mono.error(new AsientoNotFoundException(
                                "Asiento %s no encontrado".formatted(id)))))
                .flatMap(asiento -> validarDisponibleParaEvento(asiento, eventoId))
                .collectList()
                .flatMap(asientos -> bloquearTodos(asientos, eventoId, destinatario, fechaExpiracion));
    }

    private Mono<Asiento> validarDisponibleParaEvento(Asiento asiento, UUID eventoId) {
        if (!asiento.isDisponible()) {
            return Mono.error(new AsientoOcupadoException(asiento.getId()));
        }
        return Mono.zip(
                bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement(),
                asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement()
        ).flatMap(tuple -> {
            if (tuple.getT1()) {
                return Mono.error(new AsientoYaBloqueadoException(asiento.getId()));
            }
            if (tuple.getT2()) {
                return Mono.error(new AsientoOcupadoException(asiento.getId()));
            }
            return Mono.just(asiento);
        });
    }

    private Mono<List<Bloqueo>> bloquearTodos(List<Asiento> asientos, UUID eventoId,
                                               String destinatario, LocalDateTime fechaExpiracion) {
        return Flux.fromIterable(asientos)
                .flatMap(asiento -> crearBloqueo(asiento.getId(), eventoId, destinatario, fechaExpiracion))
                .collectList();
    }

    private Mono<Bloqueo> crearBloqueo(UUID asientoId, UUID eventoId,
                                        String destinatario, LocalDateTime fechaExpiracion) {
        Bloqueo bloqueo = Bloqueo.builder()
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
