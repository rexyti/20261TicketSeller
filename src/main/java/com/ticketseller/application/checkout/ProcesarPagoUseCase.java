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
import com.ticketseller.domain.model.ticket.CategoriaTicket;
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
import java.util.List;
import java.util.UUID;
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
                        obtenerCompuerta(venta.getZonaId()),
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
                                    holds.isEmpty() ? null : holds.get(i).getAsientoId()))
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

    private Mono<Compuerta> obtenerCompuerta(UUID zonaId) {
        return compuertaRepositoryPort.buscarPorZonaId(zonaId).next().defaultIfEmpty(Compuerta.builder().build());
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
                .categoria(compuerta.isEsGeneral() ? CategoriaTicket.GENERAL : CategoriaTicket.VIP)
                .zona(zona.getNombre())
                .compuerta(compuerta.getNombre())
                .fechaEvento(evento.getFechaInicio())
                .build();
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
