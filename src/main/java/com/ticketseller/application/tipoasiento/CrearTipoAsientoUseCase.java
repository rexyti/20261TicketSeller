package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.NombreTipoAsientoVacioException;
import com.ticketseller.domain.model.EstadoTipoAsiento;
import com.ticketseller.domain.model.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearTipoAsientoUseCase {
    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<Tuple2<TipoAsiento, String>> ejecutar(String nombre, String descripcion) {
        if (nombre == null || nombre.isBlank()) {
            return Mono.error(new NombreTipoAsientoVacioException("El campo nombre es obligatorio"));
        }

        String nombreNormalizado = nombre.trim();

        return tipoAsientoRepositoryPort.buscarPorNombre(nombreNormalizado)
                .map(existente -> true)
                .defaultIfEmpty(false)
                .flatMap(existe -> {
                    TipoAsiento nuevo = TipoAsiento.builder()
                            .id(UUID.randomUUID())
                            .nombre(nombreNormalizado)
                            .descripcion(descripcion != null ? descripcion.trim() : null)
                            .estado(EstadoTipoAsiento.ACTIVO)
                            .build();

                    String advertencia = existe ? "Ya existe un tipo de asiento con este nombre" : null;

                    return tipoAsientoRepositoryPort.guardar(nuevo)
                            .map(guardado -> Tuples.of(guardado, advertencia != null ? advertencia : ""));
                });
    }
}
