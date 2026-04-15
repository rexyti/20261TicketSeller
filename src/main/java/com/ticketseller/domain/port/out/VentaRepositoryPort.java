package com.ticketseller.domain.port.out;

import com.ticketseller.domain.model.Venta;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VentaRepositoryPort {
    Mono<Venta> guardar(Venta venta);
    Mono<Venta> buscarPorId(UUID id);
    Flux<Venta> buscarVentasExpiradas(LocalDateTime ahora);
    Mono<Venta> actualizarEstado(UUID id, com.ticketseller.domain.model.EstadoVenta estado);
}
