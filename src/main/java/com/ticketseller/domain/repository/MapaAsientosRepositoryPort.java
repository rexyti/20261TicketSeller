package com.ticketseller.domain.repository;

import reactor.core.publisher.Mono;
import java.util.UUID;

public interface MapaAsientosRepositoryPort {
    Mono<Boolean> tieneZonasActivas(UUID recintoId);
}
