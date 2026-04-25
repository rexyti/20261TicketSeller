package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.Reembolso;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReembolsoRepositoryPort {
    Mono<Reembolso> save(Reembolso reembolso);
    Mono<Reembolso> findById(UUID id);
    Mono<Reembolso> findByTicketId(UUID ticketId);
    Flux<Reembolso> findByVentaId(UUID ventaId);
    Flux<Reembolso> findByEstado(EstadoReembolso estado);
}
