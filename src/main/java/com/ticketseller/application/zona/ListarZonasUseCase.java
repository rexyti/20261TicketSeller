package com.ticketseller.application.zona;

import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import reactor.core.publisher.Flux;

import java.util.UUID;

public class ListarZonasUseCase {

    private final ZonaRepositoryPort zonaRepositoryPort;

    public ListarZonasUseCase(ZonaRepositoryPort zonaRepositoryPort) {
        this.zonaRepositoryPort = zonaRepositoryPort;
    }

    public Flux<Zona> ejecutar(UUID recintoId) {
        return zonaRepositoryPort.buscarPorRecintoId(recintoId);
    }
}

