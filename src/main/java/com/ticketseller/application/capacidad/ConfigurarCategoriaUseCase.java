package com.ticketseller.application.capacidad;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConfigurarCategoriaUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID id, CategoriaRecinto categoria) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> recintoRepositoryPort.guardar(buildRecintoActualizado(recinto, categoria)));
    }

    private Recinto buildRecintoActualizado(Recinto recinto, CategoriaRecinto categoria) {
        return recinto.toBuilder().categoria(categoria).build();
    }
}

