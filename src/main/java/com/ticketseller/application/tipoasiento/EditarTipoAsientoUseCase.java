package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.asiento.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.asiento.TipoAsientoNotFoundException;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class EditarTipoAsientoUseCase {
    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<TipoAsiento> ejecutar(UUID id, String nombre, String descripcion) {
        return tipoAsientoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new TipoAsientoNotFoundException("Tipo de asiento no encontrado")))
                .flatMap(existente -> validarCambioNombre(existente, nombre).thenReturn(existente))
                .flatMap(existente -> procesarYGuardar(existente, nombre, descripcion));
    }

    private Mono<Void> validarCambioNombre(TipoAsiento existente, String nuevoNombre) {
        if (nuevoNombreInvalido(existente, nuevoNombre)) {
            return Mono.empty();
        }
        return tipoAsientoRepositoryPort.tieneAsignacionEnZona(existente.getId())
                .filter(tieneAsignaciones -> !tieneAsignaciones)
                .switchIfEmpty(Mono.error(new TipoAsientoEnUsoException("No se puede cambiar el nombre del tipo de asiento porque tiene asignaciones activas")))
                .then();
    }

    private boolean nuevoNombreInvalido(TipoAsiento existente, String nuevoNombre){
        return nuevoNombre == null || nuevoNombre.trim().equals(existente.getNombre());
    }

    private Mono<TipoAsiento> procesarYGuardar(TipoAsiento existente, String nombre, String descripcion) {
        TipoAsiento actualizado = buildTipoAsientoActualizado(existente, nombre, descripcion);
        actualizado.validarDatosRegistro();

        return tipoAsientoRepositoryPort.guardar(actualizado);
    }

    private TipoAsiento buildTipoAsientoActualizado(TipoAsiento existente, String nombre, String descripcion){
        return existente.toBuilder()
                .nombre(nombre != null ? nombre : existente.getNombre())
                .descripcion(descripcion != null ? descripcion : existente.getDescripcion())
                .build()
                .normalizarDatosRegistro();
    }
}
