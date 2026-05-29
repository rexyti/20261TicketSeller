package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class VerificarDisponibilidadUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final BloqueoRepositoryPort bloqueoRepositoryPort;

    public Mono<AsientoEnEvento> ejecutar(UUID asientoId, UUID eventoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .flatMap(asiento -> calcularEstadoEnEvento(asiento, eventoId));
    }

    private Mono<AsientoEnEvento> calcularEstadoEnEvento(Asiento asiento, UUID eventoId) {
        if (asiento.isEnMantenimiento()) {
            return Mono.just(AsientoEnEvento.desde(asiento, AsientoEnEvento.MANTENIMIENTO));
        }
        if (asiento.isEspacioVacio()) {
            return Mono.just(AsientoEnEvento.desde(asiento, AsientoEnEvento.INACTIVO));
        }

        Mono<String> estadoHold = asientoHoldRepositoryPort
                .buscarPorAsientoYEvento(asiento.getId(), eventoId)
                .collectList()
                .map(this::estadoDesdeHolds);

        Mono<Boolean> tieneBloqueado = bloqueoRepositoryPort
                .buscarActivoPorAsientoYEvento(asiento.getId(), eventoId)
                .hasElement();

        return Mono.zip(estadoHold, tieneBloqueado)
                .map(tuple -> {
                    String holdEstado = tuple.getT1();
                    boolean bloqueado = tuple.getT2();
                    String efectivo;
                    if (AsientoEnEvento.VENDIDO.equals(holdEstado)) {
                        efectivo = AsientoEnEvento.VENDIDO;
                    } else if (AsientoEnEvento.RESERVADO.equals(holdEstado)) {
                        efectivo = AsientoEnEvento.RESERVADO;
                    } else if (bloqueado) {
                        efectivo = AsientoEnEvento.BLOQUEADO;
                    } else {
                        efectivo = AsientoEnEvento.DISPONIBLE;
                    }
                    return AsientoEnEvento.desde(asiento, efectivo);
                });
    }

    private String estadoDesdeHolds(List<AsientoHold> holds) {
        boolean vendido = holds.stream().anyMatch(AsientoHold::isVendido);
        if (vendido) {
            return AsientoEnEvento.VENDIDO;
        }
        boolean reservadoActivo = holds.stream().anyMatch(AsientoHold::isActiva);
        if (reservadoActivo) {
            return AsientoEnEvento.RESERVADO;
        }
        return AsientoEnEvento.DISPONIBLE;
    }
}
