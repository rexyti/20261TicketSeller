package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import com.ticketseller.domain.model.venta.TransaccionFinanciera;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.TransaccionFinancieraPersistenceMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransaccionFinancieraRepositoryAdapter implements TransaccionFinancieraRepositoryPort {

    private final TransaccionFinancieraR2dbcRepository repository;
    private final TransaccionFinancieraPersistenceMapper mapper;

    @Override
    public Mono<TransaccionFinanciera> guardar(TransaccionFinanciera transaccion) {
        return repository.save(mapper.toEntity(transaccion)).map(mapper::toDomain);
    }
}

