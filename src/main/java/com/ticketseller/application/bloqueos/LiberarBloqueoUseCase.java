package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class LiberarBloqueoUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<Bloqueo> ejecutar(UUID bloqueoId) {
        return bloqueoRepositoryPort.buscarPorId(bloqueoId)
                .switchIfEmpty(Mono.error(new BloqueoNoEncontradoException(bloqueoId)))
                .flatMap(bloqueo -> liberarCortesiaAsociada(bloqueo.getEventoId(), bloqueo.getAsientoId())
                        .then(Mono.defer(() -> {
                            bloqueo.liberar();
                            return bloqueoRepositoryPort.guardar(bloqueo);
                        })));

    }

    private Mono<Void> liberarCortesiaAsociada(UUID eventoId, UUID asientoId) {
        return cortesiaRepositoryPort.buscarPorEventoYAsiento(eventoId, asientoId)
                .flatMap(cortesia -> {
                    cortesia.marcarNoUsada();
                    return liberarTicketAsociado(cortesia.getTicketId())
                            .then(cortesiaRepositoryPort.guardar(cortesia));
                })
                .then();
    }

    private Mono<Void> liberarTicketAsociado(UUID ticketId) {
        return ticketRepositoryPort.buscarPorId(ticketId)
                .flatMap(ticket -> {
                    ticket.anular();
                    return ticketRepositoryPort.guardar(ticket);
                })
                .then();
    }
}
