package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CrearCompuertaUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public CrearCompuertaUseCase(CompuertaRepositoryPort compuertaRepositoryPort,
                                 RecintoRepositoryPort recintoRepositoryPort,
                                 ZonaRepositoryPort zonaRepositoryPort) {
        this.compuertaRepositoryPort = compuertaRepositoryPort;
        this.recintoRepositoryPort = recintoRepositoryPort;
        this.zonaRepositoryPort = zonaRepositoryPort;
    }

    public Mono<Compuerta> ejecutar(UUID recintoId, Compuerta request) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarZonaYGuardar(recintoId, request));
    }

    private Mono<Compuerta> validarZonaYGuardar(UUID recintoId, Compuerta request) {
        if (request.getZonaId() == null) {
            return compuertaRepositoryPort.guardar(request.toBuilder()
                    .id(UUID.randomUUID())
                    .recintoId(recintoId)
                    .esGeneral(true)
                    .build());
        }
        return zonaRepositoryPort.buscarPorId(request.getZonaId())
                .switchIfEmpty(Mono.error(new ZonaCapacidadExcedidaException("La zona indicada no existe")))
                .flatMap(zona -> compuertaRepositoryPort.guardar(request.toBuilder()
                        .id(UUID.randomUUID())
                        .recintoId(recintoId)
                        .esGeneral(false)
                        .build()));
    }
}

