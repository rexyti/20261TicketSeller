package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.CancelacionEvento;
import reactor.core.publisher.Mono;

public interface CancelacionEventoRepositoryPort {

    Mono<CancelacionEvento> guardar(CancelacionEvento cancelacionEvento);
}

