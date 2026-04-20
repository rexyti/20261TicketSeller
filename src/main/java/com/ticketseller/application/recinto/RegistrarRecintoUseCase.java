package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.exception.RecintoInvalidoException;
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
        return Mono.justOrEmpty(request)
                .switchIfEmpty(Mono.error(new RecintoInvalidoException("El request de registro es obligatorio")))
                .map(Recinto::normalizarDatosRegistro)
                .doOnNext(Recinto::validarDatosRegistro)
                .flatMap(this::validarNoDuplicado)
                .map(this::prepararNuevoRecinto)
                .flatMap(recintoRepositoryPort::guardar);
    }

    private Mono<Recinto> validarNoDuplicado(Recinto request) {
        return recintoRepositoryPort.buscarPorNombreYCiudad(request.getNombre(), request.getCiudad())
                .flatMap(existing -> Mono.<Recinto>error(new RecintoDuplicadoException("Ya existe un recinto con el mismo nombre y ciudad")))
                .switchIfEmpty(Mono.just(request));
    }

    private Recinto prepararNuevoRecinto(Recinto request) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();
    }
}
