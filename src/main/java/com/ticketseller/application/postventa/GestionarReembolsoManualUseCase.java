package com.ticketseller.application.postventa;

import com.ticketseller.domain.exception.ReembolsoFallidoException;
import com.ticketseller.domain.model.*;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class GestionarReembolsoManualUseCase {
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final PasarelaPagoPort pasarelaPagoPort;

    public Mono<Reembolso> ejecutar(UUID ticketId, TipoReembolso tipo, UUID agenteId) {
        return reembolsoRepositoryPort.findByTicketId(ticketId)
                .switchIfEmpty(crearReembolsoInicial(ticketId, tipo))
                .flatMap(reembolso -> {
                    if (reembolso.getEstado() == EstadoReembolso.COMPLETADO) {
                        return Mono.just(reembolso);
                    }
                    
                    return pasarelaPagoPort.procesarReembolso(reembolso.getVentaId(), reembolso.getMonto())
                            .flatMap(resultado -> {
                                if (resultado.aprobado()) {
                                    return completarReembolso(reembolso, agenteId);
                                } else {
                                    return marcarReembolsoFallido(reembolso, resultado.respuestaPasarela());
                                }
                            });
                });
    }

    public Mono<Void> procesarCola() {
        return reembolsoRepositoryPort.findByEstado(EstadoReembolso.PENDIENTE)
                .flatMap(reembolso -> {
                    if (reembolso.getTicketId() != null) {
                        return ejecutar(reembolso.getTicketId(), reembolso.getTipo(), null);
                    }
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Reembolso> crearReembolsoInicial(UUID ticketId, TipoReembolso tipo) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .map(ticket -> Reembolso.builder()
                        .id(UUID.randomUUID())
                        .ticketId(ticket.getId())
                        .ventaId(ticket.getVentaId())
                        .monto(ticket.getPrecio())
                        .tipo(tipo)
                        .estado(EstadoReembolso.PENDIENTE)
                        .fechaSolicitud(LocalDateTime.now())
                        .build())
                .flatMap(reembolsoRepositoryPort::save);
    }

    private Mono<Reembolso> completarReembolso(Reembolso reembolso, UUID agenteId) {
        Reembolso completado = reembolso.toBuilder()
                .estado(EstadoReembolso.COMPLETADO)
                .fechaCompletado(LocalDateTime.now())
                .agenteId(agenteId)
                .build();
        
        return reembolsoRepositoryPort.save(completado)
                .flatMap(r -> {
                    if (r.getTicketId() != null) {
                        return ticketRepositoryPort.buscarPorId(r.getTicketId())
                                .flatMap(ticket -> ticketRepositoryPort.guardar(ticket.toBuilder().estado(EstadoTicket.REEMBOLSADO).build()))
                                .thenReturn(r);
                    }
                    return Mono.just(r);
                });
    }

    private Mono<Reembolso> marcarReembolsoFallido(Reembolso reembolso, String motivo) {
        Reembolso fallido = reembolso.toBuilder()
                .estado(EstadoReembolso.FALLIDO)
                .build();
        return reembolsoRepositoryPort.save(fallido)
                .then(Mono.error(new ReembolsoFallidoException("El reembolso falló en la pasarela: " + motivo)));
    }
}
