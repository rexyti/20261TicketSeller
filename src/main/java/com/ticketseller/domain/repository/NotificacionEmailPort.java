package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface NotificacionEmailPort {

    Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets, String emailComprador);

    Mono<Void> enviarCancelacionTicket(Ticket ticket, String motivo, String emailComprador);

    Mono<Void> enviarReembolsoCompletado(Venta venta, Ticket ticket, BigDecimal monto, String plazoAcreditacion, String emailComprador);

    Mono<Void> enviarAlertaSoporteReembolsoFallido(UUID reembolsoId, String detalle);

    Mono<Void> enviarCortesiaGenerada(Cortesia cortesia, String nombreEvento, String emailDestinatario);
}

