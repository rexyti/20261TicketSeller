package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Venta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VentaRepositoryPort {

    Mono<Venta> guardar(Venta venta);

    Mono<Venta> buscarPorId(UUID id);

    Flux<Venta> buscarVentasExpiradas(LocalDateTime fechaCorte);

    Mono<Venta> actualizarEstado(UUID id, EstadoVenta estado);

    Flux<Venta> buscarPorComprador(UUID compradorId);
}

