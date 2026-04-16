package com.ticketseller.infrastructure.adapter.out.persistence.recinto;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class RecintoRepositoryAdapter implements RecintoRepositoryPort {

    private final RecintoR2dbcRepository repository;
    private final RecintoPersistenceMapper mapper;

    public RecintoRepositoryAdapter(RecintoR2dbcRepository repository, RecintoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Recinto> guardar(Recinto recinto) {
        return repository.save(mapper.toEntity(recinto)).map(mapper::toDomain);
    }

    @Override
    public Mono<Recinto> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Recinto> buscarPorNombreYCiudad(String nombre, String ciudad) {
        return repository.findByNombreIgnoreCaseAndCiudadIgnoreCase(nombre, ciudad).map(mapper::toDomain);
    }

    @Override
    public Flux<Recinto> listarTodos() {
        return repository.findAll().map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> tieneEventosFuturos(UUID recintoId) {
        // TODO: integrar con entidad Evento
        return Mono.just(false);
    }

    @Override
    public Flux<Recinto> buscarPorCategoria(CategoriaRecinto categoria) {
        return repository.findByCategoriaIgnoreCase(categoria.name()).map(mapper::toDomain);
    }

    @Override
    public Flux<Recinto> buscarPorCiudad(String ciudad) {
        return repository.findByCiudadIgnoreCase(ciudad).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> tieneTicketsVendidos(UUID recintoId) {
        // TODO: integrar con entidad Ticket
        return Mono.just(false);
    }
}

