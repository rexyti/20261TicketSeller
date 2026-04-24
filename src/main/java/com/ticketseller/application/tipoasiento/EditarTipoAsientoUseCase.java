package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.TipoAsientoNotFoundException;
import com.ticketseller.domain.model.TipoAsiento;
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
                .flatMap(existente -> validarCambioNombre(existente, nombre)
                        .then(Mono.defer(() -> {
                            TipoAsiento actualizado = existente.toBuilder()
                                    .nombre(nombre != null ? nombre.trim() : existente.getNombre())
                                    .descripcion(descripcion != null ? descripcion.trim() : existente.getDescripcion())
                                    .build();
                            return tipoAsientoRepositoryPort.guardar(actualizado);
                        })));
    }

    private Mono<Void> validarCambioNombre(TipoAsiento existente, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.trim().equals(existente.getNombre())) {
            return Mono.empty();
        }
        return tipoAsientoRepositoryPort.tieneAsignacionEnZona(existente.getId())
                .flatMap(tieneAsignaciones -> {
                    if (tieneAsignaciones) {
                        return Mono.error(new TipoAsientoEnUsoException("No se puede cambiar el nombre del tipo de asiento porque tiene asignaciones activas"));
                    }
                    return Mono.empty();
                });
    }
}
