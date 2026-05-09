package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.venta.MetodoPagoInvalidoException;
import com.ticketseller.domain.exception.venta.PagoRechazadoException;
import com.ticketseller.domain.exception.venta.ReservaExpiradaException;
import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.EstadoPago;
import com.ticketseller.domain.model.venta.MetodoPago;
import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.model.venta.TransaccionFinanciera;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.zona.TipoZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ProcesarPagoUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final TransaccionFinancieraRepositoryPort transaccionFinancieraRepositoryPort;
    private final PasarelaPagoPort pasarelaPagoPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final CodigoQrPort codigoQrPort;
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final PrecioZonaRepositoryPort precioZonaRepositoryPort;
    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<VentaDetalle> ejecutar(UUID ventaId, ProcesarPagoCommand command) {
        if (invalidCommand(command)) {
            return Mono.error(new MetodoPagoInvalidoException("El método de pago es obligatorio"));
        }

        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> validarVentaVigente(venta)
                        .then(pasarelaPagoPort.procesarPago(venta.getId(), venta.getTotal(), command.metodoPago()))
                        .flatMap(resultado -> registrarResultadoPago(venta, command, resultado)));
    }

    private boolean invalidCommand(ProcesarPagoCommand command) {
        return command == null || command.metodoPago() == null || command.metodoPago().isBlank();
    }

    private Mono<Void> validarVentaVigente(Venta venta) {
        if (ventaNoReservada(venta)) {
            return Mono.error(new ReservaExpiradaException("La venta no se encuentra reservada"));
        }

        if (ventaExpirada(venta)) {
            return ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.EXPIRADA)
                    .then(Mono.error(new ReservaExpiradaException("La reserva ya expiro")));
        }
        return Mono.empty();
    }

    private boolean ventaNoReservada(Venta venta) {
        return !EstadoVenta.RESERVADA.equals(venta.getEstado());
    }

    private boolean ventaExpirada(Venta venta) {
        return venta.getFechaExpiracion() != null && venta.getFechaExpiracion().isBefore(LocalDateTime.now());
    }

    private Mono<VentaDetalle> registrarResultadoPago(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return resultado.aprobado()
                ? completarVenta(venta, command, resultado)
                : rechazarPago(venta, command, resultado);
    }

    private Mono<VentaDetalle> completarVenta(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return Mono.zip(
                        obtenerZona(venta.getZonaId()),
                        obtenerPrecio(venta.getEventoId(), venta.getZonaId()),
                        obtenerCompuertaBalanceada(venta.getZonaId(), venta.getEventoId()),
                        obtenerEvento(venta.getEventoId()),
                        asientoHoldRepositoryPort.buscarPorVentaId(venta.getId()).collectList()
                )
                .flatMap(tuple -> {
                    Zona zona = tuple.getT1();
                    PrecioZona precioZona = tuple.getT2();
                    Compuerta compuerta = tuple.getT3();
                    Evento evento = tuple.getT4();
                    List<AsientoHold> holds = tuple.getT5();

                    List<Ticket> tickets = IntStream.range(0, venta.getCantidad())
                            .mapToObj(i -> buildTicket(venta, compuerta, precioZona, zona, evento,
                                    i < holds.size() ? holds.get(i).getAsientoId() : null))
                            .peek(Ticket::validarDatosRegistro)
                            .toList();

                    return ticketRepositoryPort.guardarTodos(tickets)
                            .collectList()
                            .flatMap(savedTickets -> actualizarAsientosYHolds(savedTickets, holds)
                                    .then(ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.COMPLETADA))
                                    .flatMap(ventaPagada -> guardarTransaccion(ventaPagada, command, resultado)
                                            .then(notificacionEmailPort.enviarConfirmacion(ventaPagada, savedTickets))
                                            .thenReturn(new VentaDetalle(ventaPagada, savedTickets))));
                });
    }

    private Mono<Zona> obtenerZona(UUID zonaId) {
        return zonaRepositoryPort.buscarPorId(zonaId)
                .switchIfEmpty(Mono.error(new ZonaNotFoundException("Zona no encontrada")));
    }

    private Mono<PrecioZona> obtenerPrecio(UUID eventoId, UUID zonaId) {
        return precioZonaRepositoryPort.buscarPorEvento(eventoId)
                .filter(p -> zonaId.equals(p.getZonaId()))
                .next()
                .switchIfEmpty(Mono.error(new ZonaSinPrecioException("No existe precio configurado para la zona en este evento")));
    }

    private Mono<Compuerta> obtenerCompuertaBalanceada(UUID zonaId, UUID eventoId) {
        return compuertaRepositoryPort.buscarPorZonaId(zonaId)
                .collectList()
                .flatMap(compuertas -> {
                    if (compuertas.isEmpty()) {
                        return Mono.just(Compuerta.builder().build());
                    }
                    return ticketRepositoryPort.buscarPorEventoYEstados(eventoId,
                            java.util.Set.of(EstadoTicket.VENDIDO))
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

    private Mono<Evento> obtenerEvento(UUID eventoId) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new com.ticketseller.domain.exception.evento.EventoNotFoundException("Evento no encontrado")));
    }

    private Mono<Void> actualizarAsientosYHolds(List<Ticket> tickets, List<AsientoHold> holds) {
        Mono<Void> actualizarAsientos = Flux.fromIterable(tickets)
                .filter(ticket -> ticket.getAsientoId() != null)
                .flatMap(ticket -> asientoRepositoryPort.buscarPorId(ticket.getAsientoId())
                        .flatMap(asiento -> asientoRepositoryPort.guardar(
                                asiento.toBuilder().estado(EstadoAsiento.VENDIDO).build())))
                .then();

        Mono<Void> expirarHolds = Flux.fromIterable(holds)
                .flatMap(hold -> asientoHoldRepositoryPort.guardar(
                        hold.toBuilder().estado(EstadoHold.EXPIRADO).build()))
                .then();

        return actualizarAsientos.then(expirarHolds);
    }

    private Ticket buildTicket(Venta venta, Compuerta compuerta, PrecioZona precioZona,
                               Zona zona, Evento evento, UUID asientoId) {
        AccessDetails accessDetails = buildAccessDetails(evento, zona, compuerta);
        return Ticket.builder()
                .ventaId(venta.getId())
                .eventoId(venta.getEventoId())
                .zonaId(venta.getZonaId())
                .compuertaId(compuerta.getId())
                .precio(precioZona.getPrecio())
                .esCortesia(venta.isEsCortesia())
                .estado(EstadoTicket.VENDIDO)
                .codigoQr(codigoQrPort.generarCodigo(UUID.randomUUID().toString()))
                .asientoId(asientoId)
                .accessDetails(accessDetails)
                .build()
                .normalizarDatosRegistro();
    }

    private AccessDetails buildAccessDetails(Evento evento, Zona zona, Compuerta compuerta) {
        return AccessDetails.builder()
                .categoria(resolverCategoria(zona, compuerta))
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .build();
    }

    private String resolverCategoria(Zona zona, Compuerta compuerta) {
        if (zona.getTipo() != null) {
            return zona.getTipo().name();
        }
        return compuerta.isEsGeneral() ? TipoZona.GENERAL.name() : TipoZona.VIP.name();
    }

    private Mono<VentaDetalle> rechazarPago(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return guardarTransaccion(venta, command, resultado)
                .then(Mono.error(pagoRechazado(resultado.respuestaPasarela())));
    }

    private PagoRechazadoException pagoRechazado(String respuestaPasarela) {
        return new PagoRechazadoException(respuestaPasarela == null
                ? "La transacción fue rechazada por el banco"
                : respuestaPasarela);
    }

    private Mono<TransaccionFinanciera> guardarTransaccion(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        TransaccionFinanciera transaccion = buildTransaccionFinanciera(venta, command, resultado);
        transaccion.validarDatosRegistro();
        return transaccionFinancieraRepositoryPort.guardar(transaccion);
    }

    private TransaccionFinanciera buildTransaccionFinanciera(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return TransaccionFinanciera.builder()
                .ventaId(venta.getId())
                .monto(venta.getTotal())
                .metodoPago(MetodoPago.fromValor(command.metodoPago()))
                .estadoPago(EstadoPago.fromValor(resultado.estadoPago()))
                .codigoAutorizacion(resultado.codigoAutorizacion())
                .respuestaPasarela(resultado.respuestaPasarela())
                .fecha(LocalDateTime.now())
                .ip(command.ip())
                .build()
                .normalizarDatosRegistro();
    }
}
