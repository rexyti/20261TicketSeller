package com.ticketseller.domain.port.out;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RecintoRepositoryPort {

    Mono<Recinto> guardar(Recinto recinto);

    Mono<Recinto> buscarPorId(UUID id);

    Mono<Recinto> buscarPorNombreYCiudad(String nombre, String ciudad);

    Flux<Recinto> listarTodos();

    Mono<Boolean> tieneEventosFuturos(UUID recintoId);

    Flux<Recinto> buscarPorCategoria(CategoriaRecinto categoria);

    Flux<Recinto> buscarPorCiudad(String ciudad);

    Mono<Boolean> tieneTicketsVendidos(UUID recintoId);
}

