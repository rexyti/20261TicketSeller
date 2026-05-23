package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarSnapshotUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final LiquidacionQueryPort liquidacionQueryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<SnapshotLiquidacion> ejecutar(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado con id: " + eventoId)))
                .filter(Evento::isFinalizado)
                .switchIfEmpty(Mono.error(new EventoNoFinalizadoException(
                        "El evento debe estar en estado FINALIZADO para consultar el snapshot")))
                .flatMap(evento -> Mono.zip(
                        liquidacionQueryPort.obtenerSnapshotPorEvento(eventoId),
                        recintoRepositoryPort.buscarPorId(evento.getRecintoId())
                                .defaultIfEmpty(Recinto.builder().build())
                ).map(tuple -> tuple.getT1().toBuilder()
                        .nombreEvento(evento.getNombre())
                        .recintoId(evento.getRecintoId())
                        .tipoRecinto(tuple.getT2().getCategoria())
                        .build()));
    }
}
