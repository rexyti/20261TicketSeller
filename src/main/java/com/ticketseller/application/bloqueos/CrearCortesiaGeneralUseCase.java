package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CrearCortesiaGeneralUseCase {

    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario, CategoriaCortesia categoria, UUID zonaId) {
        String codigoUnico = UUID.randomUUID().toString();

        return Mono.zip(
                buscarZona(zonaId),
                eventoRepositoryPort.buscarPorId(eventoId)
                        .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado"))),
                obtenerCompuertaBalanceada(zonaId, eventoId)
        ).flatMap(tuple -> {
            Zona zona = tuple.getT1();
            Evento evento = tuple.getT2();
            Compuerta compuerta = tuple.getT3();

            Ticket ticket = crearTicket(eventoId, zonaId, codigoUnico, zona, evento, compuerta);
            return ticketRepositoryPort.guardar(ticket)
                    .flatMap(savedTicket -> {
                        Cortesia cortesia = crearCortesia(destinatario, categoria, codigoUnico, savedTicket);
                        cortesia.validar();
                        return cortesiaRepositoryPort.guardar(cortesia);
                    });
        });
    }

    private Mono<Zona> buscarZona(UUID zonaId) {
        if (zonaId == null) {
            return Mono.just(Zona.builder().build());
        }
        return zonaRepositoryPort.buscarPorId(zonaId).defaultIfEmpty(Zona.builder().build());
    }

    private Mono<Compuerta> obtenerCompuertaBalanceada(UUID zonaId, UUID eventoId) {
        if (zonaId == null) {
            return Mono.just(Compuerta.builder().build());
        }
        return compuertaRepositoryPort.buscarPorZonaId(zonaId)
                .collectList()
                .flatMap(compuertas -> {
                    if (compuertas.isEmpty()) {
                        return Mono.just(Compuerta.builder().build());
                    }
                    return ticketRepositoryPort.buscarPorEvento(eventoId)
                            .collectList()
                            .map(tickets -> seleccionarCompuertaBalanceada(compuertas, tickets));
                });
    }

    private Compuerta seleccionarCompuertaBalanceada(List<Compuerta> compuertas, List<Ticket> tickets) {
        Map<UUID, Long> conteo = tickets.stream()
                .filter(t -> t.getCompuertaId() != null)
                .collect(Collectors.groupingBy(Ticket::getCompuertaId, Collectors.counting()));
        return compuertas.stream()
                .min(Comparator.comparingLong(c -> conteo.getOrDefault(c.getId(), 0L)))
                .orElse(compuertas.getFirst());
    }

    private Ticket crearTicket(UUID eventoId, UUID zonaId, String codigoUnico,
                               Zona zona, Evento evento, Compuerta compuerta) {
        AccessDetails accessDetails = AccessDetails.builder()
                .categoria(zona.getTipo() != null ? zona.getTipo().name() : null)
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .build();
        return Ticket.builder()
                .eventoId(eventoId)
                .zonaId(zonaId)
                .compuertaId(compuerta.getId())
                .codigoQr(codigoUnico)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .accessDetails(accessDetails)
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
