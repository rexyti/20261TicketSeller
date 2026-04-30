package com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PagoR2dbcRepository extends ReactiveCrudRepository<PagoEntity, UUID> {

    Mono<PagoEntity> findByIdExternoPasarela(String idExternoPasarela);

    Mono<PagoEntity> findByVentaId(UUID ventaId);

    Flux<PagoEntity> findByEstado(String estado);

    Flux<PagoEntity> findByEstadoAndFechaCreacionBefore(String estado, LocalDateTime fechaCorte);
}
