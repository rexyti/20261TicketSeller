package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarPromocionesUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Flux<Promocion> ejecutar(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("El evento indicado no existe")))
                .thenMany(promocionRepositoryPort.buscarPorEvento(eventoId));
    }
}
