package com.ticketseller.application.compuerta;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarCompuertasUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;

    public Flux<Compuerta> ejecutar(UUID recintoId) {
        return compuertaRepositoryPort.buscarPorRecintoId(recintoId);
    }
}

