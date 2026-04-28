package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarRecaudoIncrementalUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final LiquidacionQueryPort liquidacionQueryPort;

    public Mono<Map<String, BigDecimal>> ejecutar(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado con id: " + eventoId)))
                .flatMap(evento -> liquidacionQueryPort.obtenerRecaudoPorEvento(eventoId));
    }
}
