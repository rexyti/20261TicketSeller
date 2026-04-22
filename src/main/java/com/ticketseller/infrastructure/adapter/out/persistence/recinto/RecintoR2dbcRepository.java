package com.ticketseller.infrastructure.adapter.out.persistence.recinto;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RecintoR2dbcRepository extends ReactiveCrudRepository<RecintoEntity, UUID> {

    Mono<RecintoEntity> findByNombreIgnoreCaseAndCiudadIgnoreCase(String nombre, String ciudad);

    Flux<RecintoEntity> findByCategoriaIgnoreCase(String categoria);

    Flux<RecintoEntity> findByCiudadIgnoreCase(String ciudad);
}

