package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.CodigoPromocionalPersistenceMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public class CodigoPromocionalRepositoryAdapter implements CodigoPromocionalRepositoryPort {

    private final CodigoPromocionalR2dbcRepository repository;
    private final CodigoPromocionalPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    public CodigoPromocionalRepositoryAdapter(CodigoPromocionalR2dbcRepository repository,
                                              CodigoPromocionalPersistenceMapper mapper,
                                              DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<CodigoPromocional> guardarTodos(List<CodigoPromocional> codigos) {
        return repository.saveAll(codigos.stream().map(mapper::toEntity).toList())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<CodigoPromocional> buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public Mono<CodigoPromocional> incrementarUsos(UUID id) {
        return databaseClient.sql("""
                        UPDATE codigos_promocionales
                        SET usos_actuales = usos_actuales + 1
                        WHERE id = :id
                          AND (usos_maximos IS NULL OR usos_actuales < usos_maximos)
                        RETURNING *
                        """)
                .bind("id", id)
                .map((row, metadata) -> CodigoPromocionalEntity.builder()
                        .id(row.get("id", UUID.class))
                        .codigo(row.get("codigo", String.class))
                        .promocionId(row.get("promocion_id", UUID.class))
                        .usosMaximos(row.get("usos_maximos", Integer.class))
                        .usosActuales(row.get("usos_actuales", Integer.class))
                        .fechaInicio(row.get("fecha_inicio", java.time.LocalDateTime.class))
                        .fechaFin(row.get("fecha_fin", java.time.LocalDateTime.class))
                        .estado(row.get("estado", String.class))
                        .build())
                .one()
                .map(mapper::toDomain);
    }
}
