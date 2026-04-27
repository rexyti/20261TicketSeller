package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper.AsientoPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AsientoRepositoryAdapter implements AsientoRepositoryPort {

    private final AsientoR2dbcRepository repository;
    private final AsientoPersistenceMapper mapper;

    public AsientoRepositoryAdapter(AsientoR2dbcRepository repository,
                                    AsientoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Asiento> guardar(Asiento asiento) {
        return repository.save(mapper.toEntity(asiento)).map(mapper::toDomain);
    }

    @Override
    public Flux<Asiento> guardarTodos(List<Asiento> asientos) {
        List<AsientoEntity> entities = asientos.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).map(mapper::toDomain);
    }

    @Override
    public Mono<Asiento> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Asiento> buscarPorZonaId(UUID zonaId) {
        return repository.findByZonaId(zonaId).map(mapper::toDomain);
    }

    @Override
    public Mono<Asiento> reservarConHold(UUID id, LocalDateTime expiraEn) {
        return repository.findById(id)
                .flatMap(entity -> {
                    if (!com.ticketseller.domain.model.EstadoAsiento.DISPONIBLE.name().equals(entity.getEstado())) {
                        return Mono.error(new com.ticketseller.domain.exception.AsientoReservadoPorOtroException("ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO"));
                    }
                    entity.setEstado(com.ticketseller.domain.model.EstadoAsiento.RESERVADO.name());
                    entity.setExpiraEn(expiraEn);
                    return repository.save(entity);
                })
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Asiento> liberarHold(UUID id) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setEstado(com.ticketseller.domain.model.EstadoAsiento.DISPONIBLE.name());
                    entity.setExpiraEn(null);
                    return repository.save(entity);
                })
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Asiento> marcarOcupado(UUID id) {
        return repository.findById(id)
                .flatMap(entity -> {
                    entity.setEstado(com.ticketseller.domain.model.EstadoAsiento.OCUPADO.name());
                    entity.setExpiraEn(null);
                    return repository.save(entity);
                })
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Asiento> findHoldsVencidos(LocalDateTime ahora) {
        return repository.findHoldsVencidos(ahora).map(mapper::toDomain);
    }
}
