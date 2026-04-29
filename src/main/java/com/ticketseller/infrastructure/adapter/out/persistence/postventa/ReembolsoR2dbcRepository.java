package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReembolsoR2dbcRepository extends ReactiveCrudRepository<ReembolsoEntity, UUID> {
    Mono<ReembolsoEntity> findFirstByTicketIdOrderByFechaSolicitudDesc(UUID ticketId);

    Flux<ReembolsoEntity> findByVentaId(UUID ventaId);

    Flux<ReembolsoEntity> findByEstado(String estado);
}

