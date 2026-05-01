package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CodigoPromocionalR2dbcRepository extends ReactiveCrudRepository<CodigoPromocionalEntity, UUID> {

    Mono<CodigoPromocionalEntity> findByCodigo(String codigo);
}
