package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NotificacionEmailPort {

    Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets);
}

