package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCortesiaConAsientoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario, CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNotFoundException(
                        "Asiento %s no encontrado".formatted(asientoId))))
                .flatMap(this::validarYBloquear)
                .flatMap(asiento -> crearTicketYCortesia(eventoId, destinatario, categoria, asiento));
    }

    private Mono<Asiento> validarYBloquear(Asiento asiento) {
        return Mono.just(asiento)
                .filter(a -> EstadoAsiento.DISPONIBLE.equals(a.getEstado()))
                .switchIfEmpty(Mono.error(new AsientoOcupadoException(asiento.getId())))
                .flatMap(a -> asientoRepositoryPort.guardar(a.toBuilder().estado(EstadoAsiento.BLOQUEADO).build()));
    }

    private Mono<Cortesia> crearTicketYCortesia(UUID eventoId, String destinatario,
                                                 CategoriaCortesia categoria, Asiento asiento) {
        String codigoUnico = UUID.randomUUID().toString();
        Ticket ticket = crearTicket(eventoId, asiento, codigoUnico);
        return ticketRepositoryPort.guardar(ticket)
                .flatMap(savedTicket -> {
                    Cortesia cortesia = crearCortesia(destinatario, categoria, codigoUnico, savedTicket);
                    cortesia.validar();
                    return cortesiaRepositoryPort.guardar(cortesia);
                });
    }

    private Ticket crearTicket(UUID eventoId, Asiento asiento, String codigoUnico) {
        return Ticket.builder()
                .eventoId(eventoId)
                .zonaId(asiento.getZonaId())
                .asientoId(asiento.getId())
                .codigoQr(codigoUnico)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Cortesia crearCortesia(String destinatario, CategoriaCortesia categoria,
                                  String codigoUnico, Ticket ticket) {
        return Cortesia.builder()
                .eventoId(ticket.getEventoId())
                .asientoId(ticket.getAsientoId())
                .destinatario(destinatario)
                .categoria(categoria)
                .codigoUnico(codigoUnico)
                .ticketId(ticket.getId())
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
