package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface LiquidacionQueryPort {

    Mono<SnapshotLiquidacion> obtenerSnapshotPorEvento(UUID eventoId);

    Mono<Map<String, BigDecimal>> obtenerRecaudoPorEvento(UUID eventoId);
}
