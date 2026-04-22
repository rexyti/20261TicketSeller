package com.ticketseller.application.zona;

import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarZonasUseCase {

    private final ZonaRepositoryPort zonaRepositoryPort;

    public Flux<Zona> ejecutar(UUID recintoId) {
        return zonaRepositoryPort.buscarPorRecintoId(recintoId);
    }
}

