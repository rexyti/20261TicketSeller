package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.asiento.TipoAsientoInactivoException;
import com.ticketseller.domain.exception.asiento.TipoAsientoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.UUID;

@RequiredArgsConstructor
public class AsignarTipoAsientoAZonaUseCase {

    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Tuple2<Zona, String>> ejecutar(UUID recintoId, UUID zonaId, UUID tipoAsientoId) {
        return tipoAsientoRepositoryPort.buscarPorId(tipoAsientoId)
                .switchIfEmpty(Mono.error(new TipoAsientoNotFoundException("Tipo de asiento no encontrado")))
                .doOnNext(this::validarEstadoActivo)
                .flatMap(tipo -> marcarEnUso(tipo)
                        .flatMap(tipoActualizado -> zonaRepositoryPort.buscarPorId(zonaId)
                                .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada")))
                                .doOnNext(zona -> validarPertenenciaRecinto(zona, recintoId))
                                .flatMap(zona -> asignarYGuardar(zona, tipoActualizado))));
    }

    private Mono<TipoAsiento> marcarEnUso(TipoAsiento tipo) {
        if (tipo.isEnUso()) {
            return Mono.just(tipo);
        }
        return tipoAsientoRepositoryPort.guardar(tipo.toBuilder().enUso(true).build());
    }

    private void validarEstadoActivo(TipoAsiento tipo) {
        if (tipo.getEstado() != EstadoTipoAsiento.ACTIVO) {
            throw new TipoAsientoInactivoException("No se puede asignar un tipo de asiento inactivo. Actívelo primero.");
        }
    }

    private void validarPertenenciaRecinto(Zona zona, UUID recintoId) {
        if (!zona.getRecintoId().equals(recintoId)) {
            throw new ZonaNotFoundException("La zona no pertenece al recinto especificado");
        }
    }

    private Mono<Tuple2<Zona, String>> asignarYGuardar(Zona zona, TipoAsiento tipo) {
        String advertencia = zona.getTipoAsientoId() != null
                ? "Esta zona ya tenía un tipo asignado. Se ha reemplazado."
                : "";

        Zona zonaActualizada = zona.toBuilder()
                .tipoAsientoId(tipo.getId())
                .build();

        return zonaRepositoryPort.guardar(zonaActualizada)
                .flatMap(guardada -> actualizarTipoEnAsientos(guardada.getId(), tipo)
                        .thenReturn(Tuples.of(guardada, advertencia)));
    }

    private Mono<Void> actualizarTipoEnAsientos(UUID zonaId, TipoAsiento tipo) {
        return asientoRepositoryPort.buscarPorZonaId(zonaId)
                .flatMap(asiento -> asientoRepositoryPort.guardar(asiento.toBuilder().tipoAsiento(tipo).build()))
                .then();
    }
}
