package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.asiento.TipoAsientoYaExistenteException;
import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CrearTipoAsientoUseCase {
    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<TipoAsiento> ejecutar(String nombre, String descripcion) {
        return Mono.fromCallable(() -> buildTipoAsiento(nombre, descripcion))
                .map(TipoAsiento::normalizarDatosRegistro)
                .doOnNext(TipoAsiento::validarDatosRegistro)
                .flatMap(this::validarYGuardar);
    }

    private TipoAsiento buildTipoAsiento(String nombre, String descripcion) {
        return TipoAsiento.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .estado(EstadoTipoAsiento.ACTIVO)
                .build()
                .normalizarDatosRegistro();
    }

    private Mono<TipoAsiento> validarYGuardar(TipoAsiento tipoAsiento) {
        return tipoAsientoRepositoryPort.buscarPorNombre(tipoAsiento.getNombre())
                .hasElement()
                .filter(existe -> !existe)
                .switchIfEmpty(Mono.error(new TipoAsientoYaExistenteException(
                        "Ya existe un tipo de asiento con el nombre: " + tipoAsiento.getNombre())))
                .flatMap(existe -> tipoAsientoRepositoryPort.guardar(tipoAsiento));
    }
}
