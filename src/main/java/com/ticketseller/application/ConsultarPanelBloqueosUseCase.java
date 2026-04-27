package com.ticketseller.application;

import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.domain.repository.CortesiaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarPanelBloqueosUseCase {

    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final CortesiaRepositoryPort cortesiaRepositoryPort;

    public Flux<Bloqueo> consultarBloqueos(UUID eventoId) {
        return bloqueoRepositoryPort.buscarPorEventoId(eventoId);
    }

    public Flux<Bloqueo> consultarBloqueosActivos(UUID eventoId) {
        return bloqueoRepositoryPort.buscarActivosPorEventoId(eventoId);
    }

    public Flux<Cortesia> consultarCortesias(UUID eventoId) {
        return cortesiaRepositoryPort.buscarPorEventoId(eventoId);
    }
}
