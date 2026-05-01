package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.asiento.AsientoEnZonaDiferenteException;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.CategoriaTicket;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ReservarAsientosUseCase {

    private static final int TTL_MINUTOS = 15;

    private final TicketRepositoryPort ticketRepositoryPort;
    private final VentaRepositoryPort ventaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final PrecioZonaRepositoryPort precioZonaRepositoryPort;
    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<VentaDetalle> ejecutar(ReservarAsientosCommand command) {
        validarCommand(command);

        Mono<List<Asiento>> asientosMono = tieneAsientosEspecificos(command)
                ? obtenerYValidarAsientos(command)
                : Mono.just(List.of());

        return Mono.zip(
                        obtenerZona(command.zonaId()),
                        obtenerPrecio(command.eventoId(), command.zonaId()),
                        obtenerCompuerta(command.zonaId()),
                        ticketsOcupados(command.eventoId(), command.zonaId()),
                        obtenerEvento(command.eventoId()),
                        asientosMono
                )
                .flatMap(tuple -> reservar(command, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), tuple.getT5(), tuple.getT6()));
    }

    private boolean tieneAsientosEspecificos(ReservarAsientosCommand command) {
        return command.asientoIds() != null && !command.asientoIds().isEmpty();
    }

    private void validarCommand(ReservarAsientosCommand command) {
        if (solicitudReservaInvalida(command)) {
            throw new IllegalArgumentException("La solicitud de reserva es invalida");
        }
    }

    private boolean solicitudReservaInvalida(ReservarAsientosCommand command) {
        if (command == null || command.compradorId() == null || command.eventoId() == null
                || command.zonaId() == null || command.cantidad() == null || command.cantidad() <= 0) {
            return true;
        }
        return tieneAsientosEspecificos(command) && command.asientoIds().size() != command.cantidad();
    }

    private Mono<Zona> obtenerZona(UUID zonaId) {
        return zonaRepositoryPort.buscarPorId(zonaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Zona no encontrada")));
    }

    private Mono<PrecioZona> obtenerPrecio(UUID eventoId, UUID zonaId) {
        return precioZonaRepositoryPort.buscarPorEvento(eventoId)
                .filter(precioZona -> zonaId.equals(precioZona.getZonaId()))
                .next()
                .switchIfEmpty(Mono.error(new ZonaSinPrecioException("No existe precio configurado para la zona en este evento")));
    }

    private Mono<Compuerta> obtenerCompuerta(UUID zonaId) {
        return compuertaRepositoryPort.buscarPorZonaId(zonaId).next().defaultIfEmpty(Compuerta.builder().build());
    }

    private Mono<Long> ticketsOcupados(UUID eventoId, UUID zonaId) {
        return ticketRepositoryPort.contarPorEventoYZonaYEstados(eventoId, zonaId,
                Set.of(EstadoTicket.VENDIDO));
    }

    private Mono<Evento> obtenerEvento(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Evento no encontrado")));
    }

    private Mono<List<Asiento>> obtenerYValidarAsientos(ReservarAsientosCommand command) {
        return Flux.fromIterable(command.asientoIds())
                .flatMap(id -> asientoRepositoryPort.buscarPorId(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Asiento no encontrado: " + id))))
                .collectList()
                .flatMap(asientos -> validarEstadoAsientos(asientos, command.zonaId()));
    }

    private Mono<List<Asiento>> validarEstadoAsientos(List<Asiento> asientos, UUID zonaId) {
        asientos.forEach(asiento -> {
            if (asientoNoPerteneceAZona(asiento, zonaId)) {
                throw new AsientoEnZonaDiferenteException("El asiento " + asiento.getId() + " no pertenece a la zona solicitada");
            }
            if (asientoNoDisponible(asiento)){
                throw new AsientoNoDisponibleException("El asiento " + asiento.getId() + " no está disponible");
            }
        });
        return Mono.just(asientos);
    }

    private boolean asientoNoDisponible(Asiento asiento) {
        return !EstadoAsiento.DISPONIBLE.equals(asiento.getEstado());
    }

    private boolean asientoNoPerteneceAZona(Asiento asiento, UUID zonaId) {
        return !zonaId.equals(asiento.getZonaId());
    }

    private Mono<VentaDetalle> reservar(ReservarAsientosCommand command, Zona zona,
                                        PrecioZona precioZona, Compuerta compuerta, Long ocupados, Evento evento,
                                        List<Asiento> asientos) {
        if (zonaSinCuposDisponibles(ocupados, command, zona)) {
            return Mono.error(new AsientoNoDisponibleException("No hay cupos disponibles en la zona solicitada"));
        }

        BigDecimal total = precioZona.getPrecio().multiply(BigDecimal.valueOf(command.cantidad()));

        Venta venta = buildVenta(command, total);
        venta.validarDatosRegistro();

        List<Ticket> tickets = IntStream.range(0, command.cantidad())
                .mapToObj(i -> buildTicket(venta, command, compuerta, precioZona, zona, evento,
                        asientos.isEmpty() ? null : asientos.get(i)))
                .peek(Ticket::validarDatosRegistro)
                .toList();

        return ventaRepositoryPort.guardar(venta)
                .flatMap(savedVenta -> ticketRepositoryPort.guardarTodos(tickets)
                        .collectList()
                        .flatMap(savedTickets -> reservarAsientos(asientos)
                                .thenReturn(new VentaDetalle(savedVenta, savedTickets))));
    }

    private Mono<Void> reservarAsientos(List<Asiento> asientos) {
        if (asientos.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(asientos)
                .flatMap(asiento -> asientoRepositoryPort.guardar(asiento.toBuilder().estado(EstadoAsiento.RESERVADO).build()))
                .then();
    }

    private boolean zonaSinCuposDisponibles(Long ocupados, ReservarAsientosCommand command, Zona zona) {
        return ocupados + command.cantidad() > zona.getCapacidad();
    }

    private Venta buildVenta(ReservarAsientosCommand command, BigDecimal total) {
        LocalDateTime ahora = LocalDateTime.now();
        return Venta.builder()
                .id(UUID.randomUUID())
                .compradorId(command.compradorId())
                .eventoId(command.eventoId())
                .estado(EstadoVenta.RESERVADA)
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plusMinutes(TTL_MINUTOS))
                .total(total)
                .build();
    }

    private Ticket buildTicket(Venta venta, ReservarAsientosCommand command, Compuerta compuerta,
                               PrecioZona precioZona, Zona zona, Evento evento, Asiento asiento) {
        AccessDetails accessDetails = buildAccessDetails(evento, zona, compuerta);
        return Ticket.builder()
                .id(UUID.randomUUID())
                .ventaId(venta.getId())
                .eventoId(command.eventoId())
                .zonaId(command.zonaId())
                .compuertaId(compuerta.getId())
                .precio(precioZona.getPrecio())
                .esCortesia(Boolean.TRUE.equals(command.esCortesia()))
                .asientoId(asiento != null ? asiento.getId() : null)
                .accessDetails(accessDetails)
                .build()
                .normalizarDatosRegistro();
    }

    private AccessDetails buildAccessDetails(Evento evento, Zona zona, Compuerta compuerta) {
        return AccessDetails.builder()
                .categoria(compuerta.isEsGeneral() ? CategoriaTicket.GENERAL : CategoriaTicket.VIP)
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .build();
    }
}
