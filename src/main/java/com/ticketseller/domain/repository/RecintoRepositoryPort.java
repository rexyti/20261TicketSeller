package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.ConfiguracionLiquidacion;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.shared.Pagina;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RecintoRepositoryPort {

    Mono<Recinto> guardar(Recinto recinto);

    Mono<Recinto> buscarPorId(UUID id);

    Mono<Recinto> buscarPorNombreYCiudad(String nombre, String ciudad);

    Flux<Recinto> listarTodos();

    Mono<Pagina<Recinto>> listarFiltrados(String nombre,
                                          CategoriaRecinto categoria,
                                          String ciudad,
                                          Boolean activo,
                                          int page,
                                          int size,
                                          String sort);

    Mono<Boolean> tieneEventosFuturos(UUID recintoId);

    Flux<Recinto> buscarPorCategoria(CategoriaRecinto categoria);

    Flux<Recinto> buscarPorCiudad(String ciudad);

    Mono<Boolean> tieneTicketsVendidos(UUID recintoId);

    Mono<ConfiguracionLiquidacion> buscarConfiguracionLiquidacion(UUID recintoId);
}


