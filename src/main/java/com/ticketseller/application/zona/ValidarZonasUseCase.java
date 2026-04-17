package com.ticketseller.application.zona;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ValidarZonasUseCase {

    private final ZonaRepositoryPort zonaRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Boolean> ejecutar(UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> zonaRepositoryPort.sumarCapacidadesPorRecinto(recintoId)
                        .map(suma -> suma <= recinto.getCapacidadMaxima()));
    }
}

