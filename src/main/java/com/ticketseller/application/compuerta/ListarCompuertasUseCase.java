package com.ticketseller.application.compuerta;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import reactor.core.publisher.Flux;

import java.util.UUID;

public class ListarCompuertasUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;

    public ListarCompuertasUseCase(CompuertaRepositoryPort compuertaRepositoryPort) {
        this.compuertaRepositoryPort = compuertaRepositoryPort;
    }

    public Flux<Compuerta> ejecutar(UUID recintoId) {
        return compuertaRepositoryPort.buscarPorRecintoId(recintoId);
    }
}

