package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface TransaccionFinancieraR2dbcRepository extends ReactiveCrudRepository<TransaccionFinancieraEntity, UUID> {
}

