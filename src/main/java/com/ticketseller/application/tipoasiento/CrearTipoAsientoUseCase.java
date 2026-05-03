package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.asiento.TipoAsientoYaExistenteException;
import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

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
                .id(UUID.randomUUID())
                .nombre(nombre)
                .descripcion(descripcion)
                .estado(EstadoTipoAsiento.ACTIVO)
                .build();
    }

    private Mono<TipoAsiento> validarYGuardar(TipoAsiento tipoAsiento) {
        return tipoAsientoRepositoryPort.buscarPorNombre(tipoAsiento.getNombre())
                .hasElement()
                .flatMap(existe -> {
                    if (existe) {
                        return Mono.error(new TipoAsientoYaExistenteException(
                                "Ya existe un tipo de asiento con el nombre: " + tipoAsiento.getNombre()));
                    }
                    return tipoAsientoRepositoryPort.guardar(tipoAsiento);
                });
    }
}
