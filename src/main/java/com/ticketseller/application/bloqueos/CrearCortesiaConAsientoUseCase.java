package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
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
public class CrearCortesiaConAsientoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario, CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNotFoundException(
                        "Asiento %s no encontrado".formatted(asientoId))))
                .flatMap(this::validarYBloquear)
                .flatMap(asiento -> crearTicketYCortesia(eventoId, destinatario, categoria, asiento));
    }

    private Mono<Asiento> validarYBloquear(Asiento asiento) {
        return Mono.just(asiento)
                .filter(Asiento::isDisponible)
                .switchIfEmpty(Mono.error(new AsientoOcupadoException(asiento.getId())))
                .flatMap(a -> asientoRepositoryPort.guardar(a.toBuilder().estado(EstadoAsiento.BLOQUEADO).build()));
    }

    private Mono<Cortesia> crearTicketYCortesia(UUID eventoId, String destinatario,
                                                CategoriaCortesia categoria, Asiento asiento) {
        String codigoUnico = UUID.randomUUID().toString();

        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .flatMap(evento -> Mono.zip(
                        zonaRepositoryPort.buscarPorId(asiento.getZonaId()).defaultIfEmpty(Zona.builder().build()),
                        obtenerCompuertaBalanceada(asiento.getZonaId(), evento.getRecintoId(), eventoId)
                ).flatMap(tuple -> {
                    Zona zona = tuple.getT1();
                    Compuerta compuerta = tuple.getT2();

                    Ticket ticket = crearTicket(eventoId, asiento, codigoUnico, zona, evento, compuerta);
                    return ticketRepositoryPort.guardar(ticket)
                            .flatMap(savedTicket -> {
                                Cortesia cortesia = crearCortesia(destinatario, categoria, codigoUnico, savedTicket);
                                cortesia.validar();
                                return cortesiaRepositoryPort.guardar(cortesia);
                            });
                }));
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

    private Ticket crearTicket(UUID eventoId, Asiento asiento, String codigoUnico,
                               Zona zona, Evento evento, Compuerta compuerta) {
        AccessDetails accessDetails = AccessDetails.builder()
                .categoria(zona.getTipo() != null ? zona.getTipo().name() : null)
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .asiento(asiento.getNumero())
                .permiteReingreso(evento.isReingresoHabilitado())
                .build();
        return Ticket.builder()
                .eventoId(eventoId)
                .zonaId(asiento.getZonaId())
                .asientoId(asiento.getId())
                .compuertaId(compuerta.getId())
                .codigoQr(codigoUnico)
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .accessDetails(accessDetails)
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
