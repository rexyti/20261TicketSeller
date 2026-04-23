package com.ticketseller.application.precios;

import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.model.PrecioZona;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarPreciosUseCase {
    private final EventoRepositoryPort eventoRepositoryPort;
    private final PrecioZonaRepositoryPort precioZonaRepositoryPort;

    public Flux<PrecioZona> ejecutar(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .flatMapMany(evento -> precioZonaRepositoryPort.buscarPorEvento(eventoId));
    }
}
