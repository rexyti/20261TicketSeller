package com.ticketseller.application.mantenimiento;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.model.TransicionEstadoAsiento;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoMasivoResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoUseCase {
    private final AsientoRepositoryPort asientoRepositoryPort;
    private final HistorialCambioEstadoRepositoryPort historialRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<CambiarEstadoMasivoResponse> ejecutar(UUID eventoId, List<UUID> asientoIds, EstadoAsiento estadoDestino, String motivo, String usuarioId) {
        List<String> mensajes = new ArrayList<>();
        
        return Flux.fromIterable(asientoIds)
                .flatMap(asientoId -> asientoRepositoryPort.buscarPorId(asientoId)
                        .switchIfEmpty(Mono.defer(() -> {
                            mensajes.add(String.format("Asiento %s no encontrado", asientoId));
                            return Mono.empty();
                        }))
                        .flatMap(asiento -> {
                            if (!TransicionEstadoAsiento.esPermitida(asiento.getEstado(), estadoDestino)) {
                                mensajes.add(String.format("Asiento %s en estado %s no puede ser modificado a %s", asiento.getId(), asiento.getEstado(), estadoDestino));
                                return Mono.just(false);
                            }
                            
                            return verificarCompraActiva(asiento.getId())
                                    .flatMap(enCompra -> {
                                        if (enCompra) {
                                            mensajes.add(String.format("Asiento %s está en una compra activa", asiento.getId()));
                                            return Mono.just(false);
                                        }
                                        
                                        EstadoAsiento estadoAnterior = asiento.getEstado();
                                        Asiento asientoActualizado = asiento.toBuilder().estado(estadoDestino).build();
                                        
                                        return asientoRepositoryPort.guardar(asientoActualizado)
                                                .flatMap(guardado -> guardarHistorial(eventoId, guardado, estadoAnterior, motivo, usuarioId))
                                                .thenReturn(true);
                                    });
                        })
                )
                .reduce(new int[]{0, 0}, (acumulador, fueModificado) -> {
                    if (Boolean.TRUE.equals(fueModificado)) {
                        acumulador[0]++; // modificados
                    } else {
                        acumulador[1]++; // omitidos
                    }
                    return acumulador;
                })
                .map(resultados -> {
                    // Los asientos no encontrados se cuentan implícitamente en omitidos por los mensajes
                    int noEncontrados = asientoIds.size() - (resultados[0] + resultados[1]);
                    return new CambiarEstadoMasivoResponse(resultados[0], resultados[1] + noEncontrados, mensajes);
                });
    }

    private Mono<Boolean> verificarCompraActiva(UUID asientoId) {
        return ticketRepositoryPort.buscarPorAsiento(asientoId)
                .map(ticket -> !EstadoTicket.CANCELADO.equals(ticket.getEstado()) && !EstadoTicket.ANULADO.equals(ticket.getEstado()) && !EstadoTicket.REEMBOLSADO.equals(ticket.getEstado()))
                .defaultIfEmpty(false);
    }

    private Mono<HistorialCambioEstado> guardarHistorial(UUID eventoId, Asiento asiento, EstadoAsiento estadoAnterior, String motivo, String usuarioId) {
        HistorialCambioEstado historial = HistorialCambioEstado.builder()
                .id(UUID.randomUUID())
                .asientoId(asiento.getId())
                .eventoId(eventoId)
                .usuarioId(usuarioId)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(asiento.getEstado())
                .fechaHora(Instant.now())
                .motivo(motivo)
                .build();
        return historialRepositoryPort.guardar(historial);
    }
}
