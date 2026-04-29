package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReembolsoRepositoryPort {
    Mono<Reembolso> guardar(Reembolso reembolso);

    Flux<Reembolso> guardarTodos(Iterable<Reembolso> reembolsos);

    Mono<Reembolso> buscarPorId(UUID id);

    Mono<Reembolso> buscarPorTicketId(UUID ticketId);

    Flux<Reembolso> buscarPorVentaId(UUID ventaId);

    Flux<Reembolso> buscarPorEstado(EstadoReembolso estado);
}

