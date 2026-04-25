package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.model.SnapshotLiquidacion;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarSnapshotUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final LiquidacionQueryPort liquidacionQueryPort;

    // TODO: coordinar con Módulo 2 cómo se registra el check-in en el ticket
    public Mono<SnapshotLiquidacion> ejecutar(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado con id: " + eventoId)))
                .flatMap(evento -> {
                    if (eventoFinalizado(evento)) {
                        return Mono.error(new EventoNoFinalizadoException(
                                "El evento debe estar en estado FINALIZADO para consultar el snapshot"));
                    }
                    return liquidacionQueryPort.obtenerSnapshotPorEvento(eventoId);
                });
    }

    private boolean eventoFinalizado(Evento evento) {
        return !EstadoEvento.FINALIZADO.equals(evento.getEstado());
    }
}
