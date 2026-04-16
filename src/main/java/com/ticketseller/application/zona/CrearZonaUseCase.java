package com.ticketseller.application.zona;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class CrearZonaUseCase {

    private final ZonaRepositoryPort zonaRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;

    public CrearZonaUseCase(ZonaRepositoryPort zonaRepositoryPort, RecintoRepositoryPort recintoRepositoryPort) {
        this.zonaRepositoryPort = zonaRepositoryPort;
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Zona> ejecutar(UUID recintoId, Zona zona) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarYCrear(recinto, zona));
    }

    private Mono<Zona> validarYCrear(Recinto recinto, Zona zona) {
        return zonaRepositoryPort.buscarPorRecintoId(recinto.getId())
                .filter(existing -> existing.getNombre().equalsIgnoreCase(zona.getNombre()))
                .hasElements()
                .flatMap(existeNombre -> {
                    if (existeNombre) {
                        return Mono.error(new ZonaCapacidadExcedidaException("Ya existe una zona con ese nombre en el recinto"));
                    }
                    return zonaRepositoryPort.sumarCapacidadesPorRecinto(recinto.getId())
                            .flatMap(suma -> {
                                int nuevaSuma = suma + zona.getCapacidad();
                                if (nuevaSuma > recinto.getCapacidadMaxima()) {
                                    return Mono.error(new ZonaCapacidadExcedidaException("La capacidad de zonas excede la capacidad total del recinto"));
                                }
                                return zonaRepositoryPort.guardar(zona.toBuilder()
                                        .id(UUID.randomUUID())
                                        .recintoId(recinto.getId())
                                        .build());
                            });
                });
    }
}

