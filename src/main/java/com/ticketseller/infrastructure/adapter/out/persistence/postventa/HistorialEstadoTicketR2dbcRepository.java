package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface HistorialEstadoTicketR2dbcRepository
        extends ReactiveCrudRepository<HistorialEstadoTicketEntity, UUID> {
}

