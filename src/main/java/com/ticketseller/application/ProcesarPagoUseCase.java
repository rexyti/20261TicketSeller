package com.ticketseller.application;

import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.exception.ReservaExpiradaException;
import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.TransaccionFinanciera;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.NotificacionEmailPort;
import com.ticketseller.domain.port.out.PasarelaPagoPort;
import com.ticketseller.domain.port.out.PasarelaPagoPort.RespuestaPago;
import com.ticketseller.domain.port.out.TicketRepositoryPort;
import com.ticketseller.domain.port.out.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcesarPagoUseCase {

    private final VentaRepositoryPort ventaRepository;
    private final TicketRepositoryPort ticketRepository;
    private final PasarelaPagoPort pasarelaPago;
    private final NotificacionEmailPort emailNotificacion;
    private final QrGeneratorService qrGenerator;

    public Mono<Venta> ejecutar(UUID ventaId, String metodoPago) {
        return ventaRepository.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException(ventaId)))
                .flatMap(this::validarVentaReservada)
                .flatMap(venta -> procesarPagoConPasarela(venta, metodoPago)
                        .flatMap(respuesta -> completarVenta(venta, respuesta, metodoPago))
                        .onErrorResume(e -> manejarPagoFallido(venta, e)));
    }

    private Mono<Venta> validarVentaReservada(Venta venta) {
        LocalDateTime ahora = LocalDateTime.now();
        if (venta.estaExpirada(ahora)) {
            return Mono.error(new ReservaExpiradaException(venta.getId()));
        }
        if (venta.getEstado() != EstadoVenta.RESERVADA) {
            return Mono.error(new IllegalStateException(
                    "La venta " + venta.getId() + " no está en estado RESERVADA"));
        }
        return Mono.just(venta);
    }

    private Mono<RespuestaPago> procesarPagoConPasarela(Venta venta, String metodoPago) {
        return pasarelaPago.procesarPago(venta.getId(), venta.getTotal(), metodoPago);
    }

    private Mono<Venta> completarVenta(Venta venta, RespuestaPago respuesta, String metodoPago) {
        if (!respuesta.aprobada()) {
            return Mono.error(new PagoRechazadoException(respuesta.mensaje()));
        }

        return ventaRepository.actualizarEstado(venta.getId(), EstadoVenta.COMPLETADA)
                .flatMap(v -> completarTickets(venta.getId())
                        .flatMap(tickets -> registrarTransaccion(venta, respuesta, metodoPago)
                                .then(enviarEmailConfirmacion(v, tickets))
                                .thenReturn(v)));
    }

    private Mono<List<Ticket>> completarTickets(UUID ventaId) {
        return ticketRepository.buscarPorVenta(ventaId)
                .flatMap(ticket -> {
                    String qrCode = qrGenerator.generarQR(ticket.getId().toString());
                    Ticket ticketConQR = ticket.withCodigoQR(qrCode);
                    Ticket ticketVendido = ticketConQR.withEstado(EstadoTicket.VENDIDO);
                    return ticketRepository.guardar(ticketVendido);
                })
                .collectList();
    }

    private Mono<TransaccionFinanciera> registrarTransaccion(Venta venta, RespuestaPago respuesta, String metodoPago) {
        TransaccionFinanciera transaccion = new TransaccionFinanciera(
                UUID.randomUUID(),
                venta.getId(),
                venta.getTotal(),
                metodoPago,
                respuesta.aprobada() ? "APROBADA" : "RECHAZADA",
                respuesta.codigoAutorizacion(),
                respuesta.respuestaCompleta(),
                LocalDateTime.now(),
                "0.0.0.0"
        );
        return Mono.just(transaccion);
    }

    private Mono<Void> enviarEmailConfirmacion(Venta venta, List<Ticket> tickets) {
        return emailNotificacion.enviarConfirmacion(venta, tickets)
                .doOnSuccess(v -> log.info("Email de confirmación enviado para venta {}", venta.getId()))
                .doOnError(e -> log.error("Error enviando email de confirmación para venta {}", venta.getId(), e))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Venta> manejarPagoFallido(Venta venta, Throwable error) {
        log.error("Pago fallido para venta {}: {}", venta.getId(), error.getMessage());
        return ventaRepository.actualizarEstado(venta.getId(), EstadoVenta.FALLIDA)
                .then(Mono.error(error));
    }
}
