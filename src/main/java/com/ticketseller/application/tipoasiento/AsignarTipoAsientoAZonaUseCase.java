package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.TipoAsientoInactivoException;
import com.ticketseller.domain.exception.TipoAsientoNotFoundException;
import com.ticketseller.domain.exception.ZonaNotFoundException;
import com.ticketseller.domain.model.EstadoTipoAsiento;
import com.ticketseller.domain.model.Zona;
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

    public Mono<Tuple2<Zona, String>> ejecutar(UUID recintoId, UUID zonaId, UUID tipoAsientoId) {
        return tipoAsientoRepositoryPort.buscarPorId(tipoAsientoId)
                .switchIfEmpty(Mono.error(new TipoAsientoNotFoundException("Tipo de asiento no encontrado")))
                .flatMap(tipo -> {
                    if (tipo.getEstado() != EstadoTipoAsiento.ACTIVO) {
                        return Mono.error(new TipoAsientoInactivoException(
                                "No se puede asignar un tipo de asiento inactivo. Actívelo primero."));
                    }
                    return zonaRepositoryPort.buscarPorId(zonaId)
                            .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada")))
                            .flatMap(zona -> {
                                if (!zona.getRecintoId().equals(recintoId)) {
                                    return Mono.error(new ZonaNotFoundException("La zona no pertenece al recinto especificado"));
                                }

                                String advertencia = zona.getTipoAsientoId() != null
                                        ? "Esta zona ya tenía un tipo asignado. Se ha reemplazado."
                                        : null;

                                Zona zonaActualizada = zona.toBuilder()
                                        .tipoAsientoId(tipoAsientoId)
                                        .build();

                                return zonaRepositoryPort.guardar(zonaActualizada)
                                        .map(guardada -> Tuples.of(guardada, advertencia != null ? advertencia : ""));
                            });
                });
    }
}
