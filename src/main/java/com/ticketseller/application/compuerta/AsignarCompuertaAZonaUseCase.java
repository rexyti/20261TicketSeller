package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class AsignarCompuertaAZonaUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public AsignarCompuertaAZonaUseCase(CompuertaRepositoryPort compuertaRepositoryPort,
                                        ZonaRepositoryPort zonaRepositoryPort) {
        this.compuertaRepositoryPort = compuertaRepositoryPort;
        this.zonaRepositoryPort = zonaRepositoryPort;
    }

    public Mono<Compuerta> ejecutar(UUID compuertaId, UUID zonaId) {
        return compuertaRepositoryPort.buscarPorId(compuertaId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Compuerta no encontrada")))
                .flatMap(compuerta -> zonaRepositoryPort.buscarPorId(zonaId)
                        .switchIfEmpty(Mono.error(new ZonaCapacidadExcedidaException("Zona no encontrada")))
                        .flatMap(zona -> compuertaRepositoryPort.guardar(compuerta.toBuilder()
                                .zonaId(zona.getId())
                                .esGeneral(false)
                                .build())));
    }
}

