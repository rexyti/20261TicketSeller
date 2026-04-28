package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
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
        return Mono.fromCallable(() -> buildTipoAsiento(nombre, descripcion))
                .map(TipoAsiento::normalizarDatosRegistro)
                .doOnNext(TipoAsiento::validarDatosRegistro)
                .flatMap(this::validarYGuardar);
    }

    private TipoAsiento buildTipoAsiento(String nombre, String descripcion) {
        return TipoAsiento.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .descripcion(descripcion)
                .estado(EstadoTipoAsiento.ACTIVO)
                .build();
    }

    private Mono<Tuple2<TipoAsiento, String>> validarYGuardar(TipoAsiento tipoAsiento) {
        return tipoAsientoRepositoryPort.buscarPorNombre(tipoAsiento.getNombre())
                .hasElement()
                .flatMap(existe -> guardarConAdvertencia(tipoAsiento, existe));
    }

    private Mono<Tuple2<TipoAsiento, String>> guardarConAdvertencia(TipoAsiento tipoAsiento, boolean existe) {
        String advertencia = existe ? "Ya existe un tipo de asiento con este nombre" : "";
        return tipoAsientoRepositoryPort.guardar(tipoAsiento)
                .map(guardado -> Tuples.of(guardado, advertencia));
    }
}
