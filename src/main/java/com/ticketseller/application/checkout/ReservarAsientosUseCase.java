package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.ZonaSinPrecioException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.PrecioZona;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
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

    public Mono<VentaDetalle> ejecutar(ReservarAsientosCommand command) {
        validarCommand(command);
        return Mono.zip(
                        obtenerZona(command.zonaId()),
                        obtenerPrecio(command.eventoId(), command.zonaId()),
                        obtenerCompuerta(command.zonaId()),
                        ticketsOcupados(command.eventoId(), command.zonaId())
                )
                .flatMap(tuple -> reservar(command, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    private void validarCommand(ReservarAsientosCommand command) {
        if (solicitudReservaInvalida(command)) {
            throw new IllegalArgumentException("La solicitud de reserva es invalida");
        }
    }

    private boolean solicitudReservaInvalida(ReservarAsientosCommand command) {
        return command == null || command.compradorId() == null || command.eventoId() == null
                || command.zonaId() == null || command.cantidad() == null || command.cantidad() <= 0;
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

    private Mono<VentaDetalle> reservar(ReservarAsientosCommand command,
                                        Zona zona,
                                        PrecioZona precioZona,
                                        Compuerta compuerta,
                                        Long ocupados) {
        if (zonaSinCuposDisponibles(ocupados, command, zona))
            return Mono.error(new AsientoNoDisponibleException("No hay cupos disponibles en la zona solicitada"));

        BigDecimal total = precioZona.getPrecio().multiply(BigDecimal.valueOf(command.cantidad()));

        Venta venta = buildVenta(command, total);
        venta.validarDatosRegistro();

        List<Ticket> tickets = IntStream.range(0, command.cantidad())
                .mapToObj(ignore -> buildTicket(venta, command, compuerta, precioZona))
                .peek(Ticket::validarDatosRegistro)
                .toList();

        return ventaRepositoryPort.guardar(venta)
                .flatMap(savedVenta -> ticketRepositoryPort.guardarTodos(tickets)
                        .collectList()
                        .map(savedTickets -> new VentaDetalle(savedVenta, savedTickets)));
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

    private Ticket buildTicket(Venta venta, ReservarAsientosCommand command, Compuerta compuerta, PrecioZona precioZona){
        return Ticket.builder()
                .id(UUID.randomUUID())
                .ventaId(venta.getId())
                .eventoId(command.eventoId())
                .zonaId(command.zonaId())
                .compuertaId(compuerta.getId())
                .precio(precioZona.getPrecio())
                .esCortesia(Boolean.TRUE.equals(command.esCortesia()))
                .build()
                .normalizarDatosRegistro();
    }
}
