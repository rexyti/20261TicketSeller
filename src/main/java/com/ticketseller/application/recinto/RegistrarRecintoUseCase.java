package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class RegistrarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(Recinto request) {
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
}
