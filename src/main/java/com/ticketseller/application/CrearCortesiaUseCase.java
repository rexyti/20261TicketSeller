package com.ticketseller.application;

import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoCortesia;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.CategoriaCortesia;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.CodigoQrPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCortesiaUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final CodigoQrPort codigoQrPort;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario,
                                    CategoriaCortesia categoria, UUID asientoId, UUID zonaId) {
        if (asientoId != null) {
            return crearCortesiaConAsiento(eventoId, destinatario, categoria, asientoId);
        }
        return crearCortesiaSinAsiento(eventoId, destinatario, categoria, zonaId);
    }

    private Mono<Cortesia> crearCortesiaConAsiento(UUID eventoId, String destinatario,
                                                     CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Asiento no encontrado: " + asientoId)))
                .flatMap(asiento -> {
                    validarDisponible(asiento);
                    Asiento bloqueado = asiento.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();
                    return asientoRepositoryPort.guardar(bloqueado);
                })
                .flatMap(asientoBloqueado -> generarTicketYCortesia(
                        eventoId, destinatario, categoria, asientoBloqueado.getId(), asientoBloqueado.getZonaId()));
    }

    private Mono<Cortesia> crearCortesiaSinAsiento(UUID eventoId, String destinatario,
                                                     CategoriaCortesia categoria, UUID zonaId) {
        return generarTicketYCortesia(eventoId, destinatario, categoria, null, zonaId);
    }

    private Mono<Cortesia> generarTicketYCortesia(UUID eventoId, String destinatario,
                                                    CategoriaCortesia categoria,
                                                    UUID asientoId, UUID zonaId) {
        UUID ticketId = UUID.randomUUID();
        String codigoUnico = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(eventoId)
                .zonaId(zonaId)
                .asientoId(asientoId)
                .estado(EstadoTicket.CORTESIA)
                .precio(BigDecimal.ZERO)
                .esCortesia(true)
                .codigoQr(codigoQrPort.generarCodigo(ticketId.toString()))
                .build();

        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario(destinatario)
                .categoria(categoria)
                .codigoUnico(codigoUnico)
                .ticketId(ticketId)
                .estado(EstadoCortesia.GENERADA)
                .build();

        return ticketRepositoryPort.guardar(ticket)
                .then(cortesiaRepositoryPort.guardar(cortesia));
    }

    private void validarDisponible(Asiento asiento) {
        if (EstadoAsiento.BLOQUEADO.equals(asiento.getEstado())) {
            throw new AsientoOcupadoException(
                    "El asiento %s ya está bloqueado".formatted(asiento.getId()));
        }
        if (!EstadoAsiento.DISPONIBLE.equals(asiento.getEstado())) {
            throw new AsientoOcupadoException(
                    "El asiento %s no está disponible (estado: %s)".formatted(
                            asiento.getId(), asiento.getEstado()));
        }
    }
}
