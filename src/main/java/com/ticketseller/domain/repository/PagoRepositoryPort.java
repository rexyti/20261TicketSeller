package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PagoRepositoryPort {

    Mono<Pago> guardar(Pago pago);

    Mono<Pago> buscarPorId(UUID id);

    Mono<Pago> buscarPorIdExterno(String idExternoPasarela);

    Mono<Pago> buscarPorVentaId(UUID ventaId);

    Flux<Pago> buscarPorEstado(EstadoConciliacion estado);

    Flux<Pago> buscarPendientesAnterioresA(LocalDateTime fechaCorte);

    Mono<Pago> actualizarEstado(UUID id, EstadoConciliacion estado);

    Mono<Pago> actualizarConResolucion(UUID id, EstadoConciliacion estado, UUID agenteId, String justificacion);
}
