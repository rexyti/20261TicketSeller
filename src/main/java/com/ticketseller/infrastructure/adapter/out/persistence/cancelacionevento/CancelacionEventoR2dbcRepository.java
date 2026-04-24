package com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface CancelacionEventoR2dbcRepository extends ReactiveCrudRepository<CancelacionEventoEntity, UUID> {
}

