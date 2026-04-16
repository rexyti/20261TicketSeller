package com.ticketseller.application.capacidad;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConfigurarCategoriaUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public ConfigurarCategoriaUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Recinto> ejecutar(UUID id, CategoriaRecinto categoria) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> recintoRepositoryPort.guardar(recinto.toBuilder().categoria(categoria).build()));
    }
}

