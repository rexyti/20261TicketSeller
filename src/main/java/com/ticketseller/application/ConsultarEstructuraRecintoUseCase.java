package com.ticketseller.application;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEstructuraRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Mono<Tuple2<Recinto, List<Zona>>> ejecutar(UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado: " + recintoId)))
                .flatMap(recinto -> zonaRepositoryPort.buscarPorRecintoId(recintoId)
                        .collectList()
                        .map(zonas -> Tuples.of(recinto, zonas)));
    }
}
