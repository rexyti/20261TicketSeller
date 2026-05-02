package com.ticketseller.application.bloqueos;

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
public class CrearCortesiaUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario,
                                   CategoriaCortesia categoria, UUID asientoId) {
        return Mono.justOrEmpty(asientoId)
                .flatMap(id -> crearCortesiaConAsiento(eventoId, destinatario, categoria, id))
                .switchIfEmpty(Mono.defer(() -> crearCortesiaGeneral(eventoId, destinatario, categoria)));
    }

    private Mono<Cortesia> crearCortesiaConAsiento(UUID eventoId, String destinatario,
                                                    CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Asiento %s no encontrado".formatted(asientoId))))
                .flatMap(asiento -> validarYBloquear(asiento))
                .flatMap(asiento -> crearTicketYCortesia(eventoId, destinatario, categoria, asiento));
    }

    private Mono<Asiento> validarYBloquear(Asiento asiento) {
        return Mono.just(asiento)
                .filter(a -> EstadoAsiento.DISPONIBLE.equals(a.getEstado()))
                .switchIfEmpty(Mono.error(new AsientoOcupadoException(asiento.getId())))
                .flatMap(a -> asientoRepositoryPort.guardar(
                        a.toBuilder().estado(EstadoAsiento.BLOQUEADO).build()));
    }

    private Mono<Cortesia> crearTicketYCortesia(UUID eventoId, String destinatario,
                                                 CategoriaCortesia categoria, Asiento asiento) {
        String codigoUnico = UUID.randomUUID().toString();
        Ticket ticket = buildTicketCortesia(eventoId, asiento, codigoUnico);
        return ticketRepositoryPort.guardar(ticket)
                .flatMap(savedTicket -> guardarCortesia(eventoId, destinatario, categoria,
                        asiento.getId(), codigoUnico, savedTicket.getId()));
    }

    private Ticket buildTicketCortesia(UUID eventoId, Asiento asiento, String codigoQr) {
        return Ticket.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .zonaId(asiento.getZonaId())
                .asientoId(asiento.getId())
                .codigoQr(codigoQr)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .build();
    }

    private Mono<Cortesia> guardarCortesia(UUID eventoId, String destinatario, CategoriaCortesia categoria,
                                            UUID asientoId, String codigoUnico, UUID ticketId) {
        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .asientoId(asientoId)
                .destinatario(destinatario)
                .categoria(categoria)
                .codigoUnico(codigoUnico)
                .ticketId(ticketId)
                .estado(EstadoCortesia.GENERADA)
                .build();
        return cortesiaRepositoryPort.guardar(cortesia);
    }

    private Mono<Cortesia> crearCortesiaGeneral(UUID eventoId, String destinatario, CategoriaCortesia categoria) {
        String codigoUnico = UUID.randomUUID().toString();
        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .destinatario(destinatario)
                .categoria(categoria)
                .codigoUnico(codigoUnico)
                .estado(EstadoCortesia.GENERADA)
                .build();
        return cortesiaRepositoryPort.guardar(cortesia);
    }
}
