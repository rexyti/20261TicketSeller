package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ObtenerInventarioEventoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final BloqueoRepositoryPort bloqueoRepositoryPort;

    public Flux<AsientoEnEvento> ejecutar(UUID eventoId) {
        Mono<Map<UUID, String>> holdsMap = asientoHoldRepositoryPort.buscarPorEvento(eventoId)
                .collectList()
                .map(this::construirMapaHolds);

        Mono<Set<UUID>> asientosBloqueados = bloqueoRepositoryPort
                .buscarPorEventoYEstado(eventoId, EstadoBloqueo.ACTIVO)
                .map(b -> b.getAsientoId())
                .collect(Collectors.toSet());

        return Mono.zip(holdsMap, asientosBloqueados)
                .flatMapMany(tuple -> asientoRepositoryPort.buscarPorEventoId(eventoId)
                        .map(asiento -> AsientoEnEvento.desde(asiento,
                                calcularEstado(asiento, tuple.getT1(), tuple.getT2()))));
    }

    private Map<UUID, String> construirMapaHolds(List<AsientoHold> holds) {
        Map<UUID, String> mapa = new HashMap<>();
        for (AsientoHold hold : holds) {
            String estadoNuevo = hold.getEstado().name();
            String estadoActual = mapa.get(hold.getAsientoId());
            if (estadoActual == null
                    || (AsientoEnEvento.VENDIDO.equals(estadoNuevo))
                    || (AsientoEnEvento.RESERVADO.equals(estadoNuevo) && !AsientoEnEvento.VENDIDO.equals(estadoActual))) {
                mapa.put(hold.getAsientoId(), estadoNuevo);
            }
        }
        return mapa;
    }

    private String calcularEstado(com.ticketseller.domain.model.asiento.Asiento asiento,
                                  Map<UUID, String> holdsMap, Set<UUID> asientosBloqueados) {
        return switch (asiento.getEstado()) {
            case MANTENIMIENTO -> AsientoEnEvento.MANTENIMIENTO;
            case INACTIVO -> AsientoEnEvento.INACTIVO;
            default -> {
                String holdEstado = holdsMap.get(asiento.getId());
                if (AsientoEnEvento.VENDIDO.equals(holdEstado)) {
                    yield AsientoEnEvento.VENDIDO;
                } else if (AsientoEnEvento.RESERVADO.equals(holdEstado)) {
                    yield AsientoEnEvento.RESERVADO;
                } else if (asientosBloqueados.contains(asiento.getId())) {
                    yield AsientoEnEvento.BLOQUEADO;
                } else {
                    yield AsientoEnEvento.DISPONIBLE;
                }
            }
        };
    }
}
