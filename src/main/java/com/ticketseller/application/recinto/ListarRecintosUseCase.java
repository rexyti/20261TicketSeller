package com.ticketseller.application.recinto;

import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarRecintosUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Flux<Recinto> ejecutar() {
        return recintoRepositoryPort.listarTodos().filter(Recinto::isActivo);
    }
}
