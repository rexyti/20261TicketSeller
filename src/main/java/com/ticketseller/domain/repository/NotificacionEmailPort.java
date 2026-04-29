package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface NotificacionEmailPort {

    Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets);

    Mono<Void> enviarCancelacionTicket(Ticket ticket, String motivo);

    Mono<Void> enviarReembolsoCompletado(Venta venta, Ticket ticket, BigDecimal monto, String plazoAcreditacion);

    Mono<Void> enviarAlertaSoporteReembolsoFallido(UUID reembolsoId, String detalle);
}

