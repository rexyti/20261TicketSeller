package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
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
public class CortesiaTicketCreator {

    private final TicketRepositoryPort ticketRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;
    private final CodigoQrPort codigoQrPort;
    private final NotificacionEmailPort notificacionEmailPort;

    public Mono<Cortesia> crear(UUID eventoId, UUID destinatarioId, String emailDestinatario,
                                CategoriaCortesia categoria, UUID zonaId, Asiento asiento) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .flatMap(evento -> Mono.zip(
                        buscarZona(zonaId),
                        obtenerCompuertaBalanceada(zonaId, evento.getRecintoId(), eventoId)
                ).flatMap(tuple -> crearTicketConQrYCortesia(
                        eventoId, destinatarioId, emailDestinatario, categoria, zonaId, asiento,
                        evento, tuple.getT1(), tuple.getT2())));
    }

    private Mono<Cortesia> crearTicketConQrYCortesia(UUID eventoId, UUID destinatarioId,
                                                     String emailDestinatario,
                                                     CategoriaCortesia categoria,
                                                     UUID zonaId, Asiento asiento,
                                                     Evento evento, Zona zona, Compuerta compuerta) {
        Ticket sinQr = buildTicket(eventoId, zonaId, asiento, zona, evento, compuerta);
        return ticketRepositoryPort.guardar(sinQr)
                .flatMap(saved -> {
                    String qr = codigoQrPort.generarCodigo(saved.getId().toString());
                    return ticketRepositoryPort.guardar(saved.toBuilder().codigoQr(qr).build());
                })
                .flatMap(savedTicket -> {
                    Cortesia cortesia = buildCortesia(eventoId, asiento, destinatarioId,
                            emailDestinatario, categoria, savedTicket);
                    cortesia.validar();
                    return cortesiaRepositoryPort.guardar(cortesia);
                })
                .flatMap(cortesia -> Mono.just(cortesia)
                        .filter(this::cortesiaConEmailDeDestinatario)
                        .flatMap(c -> notificacionEmailPort.enviarCortesiaGenerada(
                                c, evento.getNombre(), c.getEmailDestinatario())
                                .thenReturn(c))
                        .defaultIfEmpty(cortesia));
    }

    private boolean cortesiaConEmailDeDestinatario(Cortesia cortesia){
        return cortesia.getEmailDestinatario() != null && !cortesia.getEmailDestinatario().isBlank();
    }

    private Mono<Zona> buscarZona(UUID zonaId) {
        if (zonaId == null) {
            return Mono.just(Zona.builder().build());
        }
        return zonaRepositoryPort.buscarPorId(zonaId).defaultIfEmpty(Zona.builder().build());
    }

    private Mono<Compuerta> obtenerCompuertaBalanceada(UUID zonaId, UUID recintoId, UUID eventoId) {
        if (zonaId == null) {
            return Mono.error(new ZonaInvalidaException("La zona es obligatoria para crear una cortesía"));
        }
        return compuertaRepositoryPort.buscarPorZonaId(zonaId)
                .collectList()
                .flatMap(compuertas -> {
                    Mono<List<Compuerta>> compuertasMono = compuertas.isEmpty()
                            ? compuertaRepositoryPort.buscarPorRecintoId(recintoId).filter(Compuerta::isEsGeneral).collectList()
                            : Mono.just(compuertas);
                    return compuertasMono.flatMap(cs -> ticketRepositoryPort.buscarPorEvento(eventoId)
                            .collectList()
                            .map(tickets -> seleccionarCompuertaBalanceada(cs, tickets)));
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

    private Ticket buildTicket(UUID eventoId, UUID zonaId, Asiento asiento,
                                Zona zona, Evento evento, Compuerta compuerta) {
        AccessDetails accessDetails = AccessDetails.builder()
                .categoria(zona.getTipo() != null ? zona.getTipo().name() : null)
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .asiento(asiento != null ? asiento.getNumero() : null)
                .permiteReingreso(evento.isReingresoHabilitado())
                .build();
        return Ticket.builder()
                .eventoId(eventoId)
                .zonaId(zonaId)
                .asientoId(asiento != null ? asiento.getId() : null)
                .compuertaId(compuerta.getId())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .accessDetails(accessDetails)
                .build();
    }

    private Cortesia buildCortesia(UUID eventoId, Asiento asiento, UUID destinatarioId,
                                   String emailDestinatario, CategoriaCortesia categoria,
                                   Ticket savedTicket) {
        return Cortesia.builder()
                .eventoId(eventoId)
                .asientoId(asiento != null ? asiento.getId() : null)
                .destinatarioId(destinatarioId)
                .emailDestinatario(emailDestinatario)
                .categoria(categoria)
                .ticketId(savedTicket.getId())
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
