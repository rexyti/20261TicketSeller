package com.ticketseller.application.mantenimiento;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.RecintoEstructuraResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ConsultarEstructuraRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<RecintoEstructuraResponse> ejecutar(UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado: " + recintoId)))
                .flatMap(recinto -> zonaRepositoryPort.buscarPorRecintoId(recintoId)
                        .flatMap(zona -> {
                            if (zona.getTipoAsientoId() != null) {
                                return tipoAsientoRepositoryPort.buscarPorId(zona.getTipoAsientoId())
                                        .map(tipo -> new ZonaInfo(zona.getNombre(), tipo.getNombre()))
                                        .defaultIfEmpty(new ZonaInfo(zona.getNombre(), "GENERAL"));
                            }
                            return Mono.just(new ZonaInfo(zona.getNombre(), "GENERAL"));
                        })
                        .collectList()
                        .map(zonaInfos -> {
                            var bloques = zonaInfos.stream()
                                    .map(info -> new RecintoEstructuraResponse.BloqueResponse(
                                            info.nombre(),
                                            List.of(new RecintoEstructuraResponse.ZonaResponse(
                                                    info.nombre(),
                                                    info.categoria(),
                                                    "ACCESO-G" // Default placeholder
                                            ))
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new RecintoEstructuraResponse(recinto.getId(), bloques);
                        }));
    }

    private record ZonaInfo(String nombre, String categoria) {}
}
