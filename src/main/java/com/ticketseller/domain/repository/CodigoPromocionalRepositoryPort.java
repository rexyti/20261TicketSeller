package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface CodigoPromocionalRepositoryPort {

    Flux<CodigoPromocional> guardarTodos(List<CodigoPromocional> codigos);

    Mono<CodigoPromocional> buscarPorCodigo(String codigo);

    Mono<CodigoPromocional> incrementarUsos(UUID id);
}
