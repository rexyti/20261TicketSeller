package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarPanelBloqueosUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;

    public Flux<PanelItem> ejecutar(UUID eventoId, TipoPanelItem tipo) {
        return Flux.merge(
                bloqueosSiFiltroPermite(eventoId, tipo),
                cortesiasSiFiltroPermite(eventoId, tipo)
        );
    }

    private Flux<PanelItem> bloqueosSiFiltroPermite(UUID eventoId, TipoPanelItem tipo) {
        return Mono.just(debeIncluirBloqueos(tipo))
                .filter(Boolean::booleanValue)
                .flatMapMany(ignored -> bloqueoRepositoryPort.buscarPorEvento(eventoId))
                .map(this::toPanelItemDeBloqueo);
    }

    private Flux<PanelItem> cortesiasSiFiltroPermite(UUID eventoId, TipoPanelItem tipo) {
        return Mono.just(debeIncluirCortesias(tipo))
                .filter(Boolean::booleanValue)
                .flatMapMany(ignored -> cortesiaRepositoryPort.buscarPorEvento(eventoId))
                .map(this::toPanelItemDeCortesia);
    }

    private PanelItem toPanelItemDeBloqueo(Bloqueo b) {
        return new PanelItem(
                b.getId(), TipoPanelItem.BLOQUEO, b.getAsientoId(),
                b.getDestinatario(), b.getEstado().name(),
                b.getFechaCreacion(), b.getFechaExpiracion(), null, null);
    }

    private PanelItem toPanelItemDeCortesia(Cortesia c) {
        return new PanelItem(
                c.getId(), TipoPanelItem.CORTESIA, c.getAsientoId(),
                c.getDestinatario(), c.getEstado().name(),
                null, null, c.getCodigoUnico(),
                c.getCategoria() != null ? c.getCategoria().name() : null);
    }

    private boolean debeIncluirBloqueos(TipoPanelItem tipo) {
        return tipo == null || TipoPanelItem.BLOQUEO.equals(tipo);
    }

    private boolean debeIncluirCortesias(TipoPanelItem tipo) {
        return tipo == null || TipoPanelItem.CORTESIA.equals(tipo);
    }
}
