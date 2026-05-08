package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCortesiaGeneralUseCase {

    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario, CategoriaCortesia categoria, UUID zonaId) {
        String codigoUnico = UUID.randomUUID().toString();
        Ticket ticket = crearTicket(eventoId, zonaId, codigoUnico);
        return ticketRepositoryPort.guardar(ticket)
                .flatMap(savedTicket -> {
                    Cortesia cortesia = crearCortesia(destinatario, categoria, codigoUnico, savedTicket);
                    cortesia.validar();
                    return cortesiaRepositoryPort.guardar(cortesia);
                });
    }

    private Ticket crearTicket(UUID eventoId, UUID zonaId, String codigoUnico) {
        return Ticket.builder()
                .eventoId(eventoId)
                .zonaId(zonaId)
                .codigoQr(codigoUnico)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Cortesia crearCortesia(String destinatario, CategoriaCortesia categoria, String codigoUnico, Ticket ticket) {
        return Cortesia.builder()
                .eventoId(ticket.getEventoId())
                .destinatario(destinatario)
                .categoria(categoria)
                .codigoUnico(codigoUnico)
                .ticketId(ticket.getId())
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
