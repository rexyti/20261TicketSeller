package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface HistorialEstadoVentaRepositoryPort {

    Mono<HistorialEstadoVenta> guardar(HistorialEstadoVenta historial);

    Flux<HistorialEstadoVenta> buscarPorVentaId(UUID ventaId);
}
