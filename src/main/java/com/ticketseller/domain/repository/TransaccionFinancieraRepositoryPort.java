package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.venta.TransaccionFinanciera;
import reactor.core.publisher.Mono;

public interface TransaccionFinancieraRepositoryPort {

    Mono<TransaccionFinanciera> guardar(TransaccionFinanciera transaccion);
}

