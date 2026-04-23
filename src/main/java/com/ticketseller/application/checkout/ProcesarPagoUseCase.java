package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.exception.ReservaExpiradaException;
import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.EstadoPago;
import com.ticketseller.domain.model.MetodoPago;
import com.ticketseller.domain.model.ResultadoPago;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.TransaccionFinanciera;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ProcesarPagoUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final TransaccionFinancieraRepositoryPort transaccionFinancieraRepositoryPort;
    private final PasarelaPagoPort pasarelaPagoPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final CodigoQrPort codigoQrPort;

    public Mono<VentaDetalle> ejecutar(UUID ventaId, ProcesarPagoCommand command) {
        if (invalidCommand(command))
            return Mono.error(new IllegalArgumentException("El metodo de pago es obligatorio"));

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
        if (ventaNoReservada(venta))
            return Mono.error(new ReservaExpiradaException("La venta no se encuentra reservada"));

        if (ventaExpirada(venta)) {
            return ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.EXPIRADA)
                    .then(ticketRepositoryPort.actualizarEstadoPorVenta(venta.getId(), EstadoTicket.DISPONIBLE))
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
        return ticketRepositoryPort.buscarPorVenta(venta.getId())
                .map(this::buildTicket)
                .doOnNext(Ticket::validarDatosRegistro)
                .collectList()
                .flatMap(ticketsVendidos -> ticketRepositoryPort.guardarTodos(ticketsVendidos)
                        .collectList()
                        .flatMap(savedTickets -> ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.COMPLETADA)
                                .flatMap(ventaPagada -> guardarTransaccion(ventaPagada, command, resultado)
                                        .then(notificacionEmailPort.enviarConfirmacion(ventaPagada, savedTickets))
                                        .thenReturn(new VentaDetalle(ventaPagada, savedTickets)))));
    }

    private Ticket buildTicket(Ticket ticket) {
        return ticket.toBuilder()
                .estado(EstadoTicket.VENDIDO)
                .codigoQr(codigoQrPort.generarCodigo(ticket.getId().toString()))
                .build()
                .normalizarDatosRegistro();
    }

    private Mono<VentaDetalle> rechazarPago(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return guardarTransaccion(venta, command, resultado)
                .then(Mono.error(new PagoRechazadoException(
                        resultado.respuestaPasarela() == null
                                ? "La transaccion fue rechazada por el banco"
                                : resultado.respuestaPasarela())));
    }

    private Mono<TransaccionFinanciera> guardarTransaccion(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        TransaccionFinanciera transaccion = buildTransaccionFinanciera(venta, command, resultado);
        transaccion.validarDatosRegistro();
        return transaccionFinancieraRepositoryPort.guardar(transaccion);
    }

    private TransaccionFinanciera buildTransaccionFinanciera(Venta venta, ProcesarPagoCommand command, ResultadoPago resultado) {
        return TransaccionFinanciera.builder()
                .id(UUID.randomUUID())
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

