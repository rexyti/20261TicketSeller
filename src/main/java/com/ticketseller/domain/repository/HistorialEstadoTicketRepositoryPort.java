package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import reactor.core.publisher.Mono;

public interface HistorialEstadoTicketRepositoryPort {
    Mono<HistorialEstadoTicket> guardar(HistorialEstadoTicket historial);
}

