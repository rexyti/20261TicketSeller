package com.ticketseller.infrastructure.adapter.out.persistence.mapaasientos;

import com.ticketseller.domain.repository.MapaAsientosRepositoryPort;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class MapaAsientosRepositoryAdapter implements MapaAsientosRepositoryPort {

    private final DatabaseClient databaseClient;

    public MapaAsientosRepositoryAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Boolean> tieneZonasActivas(UUID recintoId) {
        return databaseClient.sql("SELECT EXISTS(SELECT 1 FROM zonas WHERE recinto_id = :recintoId)")
                .bind("recintoId", recintoId)
                .map(row -> row.get(0, Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }
}
