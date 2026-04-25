package com.ticketseller.application;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.RecintoEstructuraResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ConsultarEstructuraRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Mono<RecintoEstructuraResponse> ejecutar(java.util.UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado: " + recintoId)))
                .flatMap(recinto -> zonaRepositoryPort.buscarPorRecintoId(recintoId)
                        .collectList()
                        .map(zonas -> {
                            var bloques = zonas.stream()
                                    .map(zona -> new RecintoEstructuraResponse.BloqueResponse(
                                            zona.getNombre(), // Usamos el nombre de la zona como bloque por ahora
                                            java.util.List.of(new RecintoEstructuraResponse.ZonaResponse(
                                                    zona.getNombre(),
                                                    "GENERAL", // TODO: Obtener categoría real del TipoAsiento
                                                    "N/A"      // TODO: Obtener coordenada de acceso real
                                            ))
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new RecintoEstructuraResponse(recinto.getId(), bloques);
                        }));
    }
}
