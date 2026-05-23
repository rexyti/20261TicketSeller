package com.ticketseller.application.checkout;

import com.ticketseller.application.promocion.AplicarDescuentoCarritoUseCase;
import com.ticketseller.application.promocion.ItemCarrito;
import com.ticketseller.domain.exception.asiento.AsientoEnZonaDiferenteException;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.exception.venta.SolicitudReservaInvalidaException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class ReservarAsientosUseCase {

    private static final int TTL_MINUTOS = 15;

    private final TicketRepositoryPort ticketRepositoryPort;
    private final VentaRepositoryPort ventaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final PrecioZonaRepositoryPort precioZonaRepositoryPort;
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase;

    public Mono<VentaDetalle> ejecutar(ReservarAsientosCommand command) {
        validarCommand(command);

        Mono<List<Asiento>> asientosMono = tieneAsientosEspecificos(command)
                ? obtenerYValidarAsientos(command)
                : Mono.just(List.of());

        return Mono.zip(
                        obtenerZona(command.zonaId()),
                        obtenerPrecio(command.eventoId(), command.zonaId()),
                        ticketsOcupados(command.eventoId(), command.zonaId()),
                        asientosMono
                )
                .flatMap(tuple -> reservar(command, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
    }

    private boolean tieneAsientosEspecificos(ReservarAsientosCommand command) {
        return command.asientoIds() != null && !command.asientoIds().isEmpty();
    }

    private void validarCommand(ReservarAsientosCommand command) {
        if (solicitudReservaInvalida(command)) {
            throw new SolicitudReservaInvalidaException("La solicitud de reserva es invalida");
        }
    }

    private boolean solicitudReservaInvalida(ReservarAsientosCommand command) {
        return commandTieneDatosInvalidos(command) ||
                (tieneAsientosEspecificos(command) && command.asientoIds().size() != command.cantidad());
    }

    private boolean commandTieneDatosInvalidos(ReservarAsientosCommand command) {
        return command == null || command.compradorId() == null || command.eventoId() == null
                || command.zonaId() == null || command.cantidad() == null || command.cantidad() <= 0;
    }

    private Mono<Zona> obtenerZona(UUID zonaId) {
        return zonaRepositoryPort.buscarPorId(zonaId)
                .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada: %s".formatted(zonaId))));
    }

    private Mono<PrecioZona> obtenerPrecio(UUID eventoId, UUID zonaId) {
        return precioZonaRepositoryPort.buscarPorEvento(eventoId)
                .filter(precioZona -> zonaId.equals(precioZona.getZonaId()))
                .next()
                .switchIfEmpty(Mono.error(new ZonaSinPrecioException("No existe precio configurado para la zona en este evento")));
    }

    private Mono<Long> ticketsOcupados(UUID eventoId, UUID zonaId) {
        return ticketRepositoryPort.contarPorEventoYZonaYEstados(eventoId, zonaId,
                Set.of(EstadoTicket.VENDIDO));
    }

    private Mono<List<Asiento>> obtenerYValidarAsientos(ReservarAsientosCommand command) {
        return Flux.fromIterable(command.asientoIds())
                .flatMap(id -> asientoRepositoryPort.buscarPorId(id)
                        .switchIfEmpty(Mono.error(new AsientoNotFoundException("Asiento no encontrado: %s".formatted(id)))))
                .collectList()
                .flatMap(asientos -> validarEstadoAsientos(asientos, command.zonaId()));
    }

    private Mono<List<Asiento>> validarEstadoAsientos(List<Asiento> asientos, UUID zonaId) {
        asientos.forEach(asiento -> {
            if (!asiento.perteneceAZona(zonaId)) {
                throw new AsientoEnZonaDiferenteException("El asiento %s no pertenece a la zona solicitada".formatted(asiento.getId()));
            }
            if (!asiento.isDisponible()) {
                throw new AsientoNoDisponibleException("El asiento %s no está disponible".formatted(asiento.getId()));
            }
        });
        return Mono.just(asientos);
    }

    private Mono<VentaDetalle> reservar(ReservarAsientosCommand command, Zona zona,
                                        PrecioZona precioZona, Long ocupados, List<Asiento> asientos) {
        if (zonaSinCuposDisponibles(ocupados, command, zona)) {
            return Mono.error(new AsientoNoDisponibleException("No hay cupos disponibles en la zona solicitada"));
        }

        List<ItemCarrito> items = Collections.nCopies(command.cantidad(),
                new ItemCarrito(command.zonaId(), precioZona.getPrecio()));

        return aplicarDescuentoCarritoUseCase.ejecutar(command.eventoId(), command.tipoUsuario(), items)
                .flatMap(descuentoAplicado -> {
                    Venta venta = buildVenta(command, descuentoAplicado.totalFinal());
                    venta.validarDatosRegistro();

                    return ventaRepositoryPort.guardar(venta)
                            .flatMap(savedVenta -> crearHolds(savedVenta, asientos)
                                    .thenReturn(new VentaDetalle(savedVenta, List.of())));
                });
    }

    private Mono<Void> crearHolds(Venta venta, List<Asiento> asientos) {
        if (asientos.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(asientos)
                .flatMap(asiento -> {
                    AsientoHold hold = buildHold(venta, asiento);
                    return asientoHoldRepositoryPort.guardar(hold)
                            .then(asientoRepositoryPort.guardar(
                                    asiento.toBuilder().estado(EstadoAsiento.RESERVADO).build()));
                })
                .then();
    }

    private AsientoHold buildHold(Venta venta, Asiento asiento) {
        return AsientoHold.builder()
                .asientoId(asiento.getId())
                .ventaId(venta.getId())
                .numero(asiento.getNumero())
                .expiraEn(venta.getFechaExpiracion())
                .estado(EstadoHold.RESERVADO)
                .build();
    }

    private boolean zonaSinCuposDisponibles(Long ocupados, ReservarAsientosCommand command, Zona zona) {
        return ocupados + command.cantidad() > zona.getCapacidad();
    }

    private Venta buildVenta(ReservarAsientosCommand command, BigDecimal total) {
        LocalDateTime ahora = LocalDateTime.now();
        return Venta.builder()
                .compradorId(command.compradorId())
                .eventoId(command.eventoId())
                .zonaId(command.zonaId())
                .cantidad(command.cantidad())
                .esCortesia(Boolean.TRUE.equals(command.esCortesia()))
                .estado(EstadoVenta.RESERVADA)
                .fechaCreacion(ahora)
                .fechaExpiracion(ahora.plusMinutes(TTL_MINUTOS))
                .total(total)
                .build();
    }
}
