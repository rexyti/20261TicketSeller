package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.postventa.ReembolsoFallidoException;
import com.ticketseller.domain.exception.postventa.TicketNoAptoParaReembolsoException;
import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GestionarReembolsoManualUseCase {
    private static final String PLAZO_ACREDITACION = "3-5 días hábiles";
    private static final String METODO_REEMBOLSO = "TARJETA";

    private final TicketRepositoryPort ticketRepositoryPort;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;
    private final PasarelaPagoPort pasarelaPagoPort;
    private final NotificacionEmailPort notificacionEmailPort;
    private final VentaRepositoryPort ventaRepositoryPort;

    public Mono<Reembolso> ejecutar(UUID ticketId, TipoReembolso tipo, BigDecimal monto, UUID agenteId) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .switchIfEmpty(Mono.error(new TicketNotFoundException("Ticket no encontrado: " + ticketId)))
                .flatMap(this::validarEstadoTicket)
                .flatMap(ticket -> procesarReembolso(ticket, tipo, monto, agenteId));
    }

    public Mono<Void> procesarColaPendiente() {
        return reembolsoRepositoryPort.buscarPorEstado(EstadoReembolso.PENDIENTE)
                .flatMap(this::procesarDesdeCola)
                .then();
    }

    private Mono<Ticket> validarEstadoTicket(Ticket ticket) {
        if (EstadoTicket.CANCELADO.equals(ticket.getEstado()) || EstadoTicket.REEMBOLSO_PENDIENTE.equals(ticket.getEstado())) {
            return Mono.just(ticket);
        }
        return Mono.error(new TicketNoAptoParaReembolsoException("El ticket no está en estado válido para reembolso"));
    }

    private Mono<Reembolso> procesarReembolso(Ticket ticket, TipoReembolso tipo, BigDecimal monto, UUID agenteId) {
        BigDecimal montoProcesado = resolverMonto(ticket, tipo, monto);
        return reembolsoRepositoryPort.buscarPorTicketId(ticket.getId())
                .defaultIfEmpty(reembolsoNuevo(ticket, tipo == null ? TipoReembolso.TOTAL : tipo, montoProcesado, agenteId))
                .flatMap(reembolso -> marcarEnProceso(reembolso, tipo, montoProcesado, agenteId))
                .flatMap(reembolsoEnProceso -> pasarelaPagoPort
                        .procesarReembolso(ticket.getVentaId(), reembolsoEnProceso.getMonto(), METODO_REEMBOLSO)
                        .flatMap(resultado -> finalizarProceso(ticket, reembolsoEnProceso, resultado))
                        .onErrorResume(ex -> registrarFallo(reembolsoEnProceso, ex.getMessage())));
    }

    private Mono<Reembolso> procesarDesdeCola(Reembolso reembolso) {
        return ticketRepositoryPort.buscarPorId(reembolso.getTicketId())
                .flatMap(ticket -> pasarelaPagoPort.procesarReembolso(ticket.getVentaId(), reembolso.getMonto(), METODO_REEMBOLSO)
                        .flatMap(resultado -> finalizarProceso(ticket, reembolso.toBuilder().estado(EstadoReembolso.EN_PROCESO).build(),
                                resultado))
                        .onErrorResume(ex -> registrarFallo(reembolso, ex.getMessage())));
    }

    private BigDecimal resolverMonto(Ticket ticket, TipoReembolso tipo, BigDecimal monto) {
        TipoReembolso tipoEfectivo = tipo == null ? TipoReembolso.TOTAL : tipo;
        if (TipoReembolso.TOTAL.equals(tipoEfectivo)) {
            return ticket.getPrecio();
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || monto.compareTo(ticket.getPrecio()) > 0) {
            throw new IllegalArgumentException("Monto parcial inválido");
        }
        return monto;
    }

    private Reembolso reembolsoNuevo(Ticket ticket, TipoReembolso tipo, BigDecimal monto, UUID agenteId) {
        return Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticket.getId())
                .ventaId(ticket.getVentaId())
                .monto(monto)
                .tipo(tipo)
                .estado(EstadoReembolso.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .agenteId(agenteId)
                .build();
    }

    private Mono<Reembolso> marcarEnProceso(Reembolso reembolso, TipoReembolso tipo, BigDecimal monto, UUID agenteId) {
        Reembolso enProceso = reembolso.toBuilder()
                .tipo(tipo == null ? reembolso.getTipo() : tipo)
                .monto(monto)
                .agenteId(agenteId == null ? reembolso.getAgenteId() : agenteId)
                .estado(EstadoReembolso.EN_PROCESO)
                .fechaSolicitud(reembolso.getFechaSolicitud() == null ? LocalDateTime.now() : reembolso.getFechaSolicitud())
                .build();
        enProceso.validarDatosRegistro();
        return reembolsoRepositoryPort.guardar(enProceso);
    }

    private Mono<Reembolso> finalizarProceso(Ticket ticket, Reembolso reembolsoEnProceso, ResultadoPago resultado) {
        if (!resultado.aprobado()) {
            return registrarFallo(reembolsoEnProceso, resultado.respuestaPasarela());
        }
        Reembolso completado = reembolsoEnProceso.toBuilder()
                .estado(EstadoReembolso.COMPLETADO)
                .fechaCompletado(LocalDateTime.now())
                .build();
        Ticket ticketReembolsado = ticket.toBuilder().estado(EstadoTicket.REEMBOLSADO).build();
        return reembolsoRepositoryPort.guardar(completado)
                .flatMap(saved -> ticketRepositoryPort.guardar(ticketReembolsado)
                        .then(notificarComprador(saved, ticket))
                        .thenReturn(saved));
    }

    private Mono<Void> notificarComprador(Reembolso reembolso, Ticket ticket) {
        return ventaRepositoryPort.buscarPorId(ticket.getVentaId())
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada: " + ticket.getVentaId())))
                .flatMap(venta -> notificacionEmailPort.enviarReembolsoCompletado(
                        venta, ticket, reembolso.getMonto(), PLAZO_ACREDITACION));
    }

    private Mono<Reembolso> registrarFallo(Reembolso reembolso, String detalle) {
        Reembolso fallido = reembolso.toBuilder()
                .estado(EstadoReembolso.FALLIDO)
                .fechaCompletado(LocalDateTime.now())
                .build();
        return reembolsoRepositoryPort.guardar(fallido)
                .flatMap(saved -> notificacionEmailPort
                        .enviarAlertaSoporteReembolsoFallido(saved.getId(), detalle == null ? "Error en pasarela" : detalle)
                        .then(Mono.error(new ReembolsoFallidoException(detalle == null ? "Reembolso fallido" : detalle))));
    }
}

