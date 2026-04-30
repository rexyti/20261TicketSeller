package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface CodigoPromocionalRepositoryPort {

    Mono<CodigoPromocional> guardar(CodigoPromocional codigoPromocional);

    Flux<CodigoPromocional> guardarTodos(Iterable<CodigoPromocional> codigosPromocionales);

    Mono<CodigoPromocional> buscarPorCodigo(String codigo);

    Mono<Boolean> incrementarUsoAtomico(String codigo, LocalDateTime fecha);
}

