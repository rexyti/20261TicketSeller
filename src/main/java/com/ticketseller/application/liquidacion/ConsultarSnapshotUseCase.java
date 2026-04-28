package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
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
                .filter(this::eventoFinalizado)
                .switchIfEmpty(Mono.error(new EventoNoFinalizadoException(
                        "El evento debe estar en estado FINALIZADO para consultar el snapshot")))
                .flatMap(evento -> liquidacionQueryPort.obtenerSnapshotPorEvento(eventoId));
    }

    private boolean eventoFinalizado(Evento evento){
        return EstadoEvento.FINALIZADO.equals(evento.getEstado());
    }
}
