package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID id) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")));
    }
}
