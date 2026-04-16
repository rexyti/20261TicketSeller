package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class RegistrarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public RegistrarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Recinto> ejecutar(Recinto request) {
        validar(request);
        return recintoRepositoryPort.buscarPorNombreYCiudad(request.getNombre(), request.getCiudad())
                .hasElement()
                .flatMap(existe -> {
                    if (existe) {
                        return Mono.error(new RecintoDuplicadoException("Ya existe un recinto con el mismo nombre y ciudad"));
                    }
                    return recintoRepositoryPort.guardar(prepararNuevoRecinto(request));
                });
    }

    private Recinto prepararNuevoRecinto(Recinto request) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();
    }

    private void validar(Recinto request) {
        if (esVacio(request.getNombre()) || esVacio(request.getCiudad()) || esVacio(request.getDireccion())
                || esVacio(request.getTelefono()) || request.getCapacidadMaxima() == null || request.getCapacidadMaxima() < 1
                || request.getCompuertasIngreso() == null || request.getCompuertasIngreso() < 0) {
            throw new IllegalArgumentException("Datos de recinto invalidos");
        }
    }

    private boolean esVacio(String value) {
        return value == null || value.isBlank();
    }
}
