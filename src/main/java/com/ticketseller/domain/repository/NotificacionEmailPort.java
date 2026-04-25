package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import reactor.core.publisher.Mono;

import java.util.List;

public interface NotificacionEmailPort {

    Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets);
    
    Mono<Void> enviarAnulacion(Venta venta, Ticket ticket, String motivo);
}

