package com.ticketseller.application.precios;

import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.exception.ZonaSinPrecioException;
import com.ticketseller.domain.model.PrecioZona;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ConfigurarPreciosUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final PrecioZonaRepositoryPort precioZonaRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Flux<PrecioZona> ejecutar(UUID eventoId, List<PrecioZona> preciosRequest) {
        return eventoRepositoryPort.buscarPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .flatMapMany(evento -> validarYGuardar(eventoId, evento.getRecintoId(), preciosRequest));
    }

    private Flux<PrecioZona> validarYGuardar(UUID eventoId, UUID recintoId, List<PrecioZona> preciosRequest) {
        return Mono.justOrEmpty(preciosRequest)
                .filter(list -> !list.isEmpty())
                .switchIfEmpty(Mono.error(new ZonaSinPrecioException("Debe configurar precios para todas las zonas")))
                .flatMapMany(precios -> zonaRepositoryPort.buscarPorRecintoId(recintoId)
                        .collectList()
                        .flatMapMany(zonas -> {
                            Map<UUID, PrecioZona> porZona = obtenerPreciosPorZona(precios);

                            if (hayPreciosIncompletos(zonas, porZona)) {
                                return Flux.error(new ZonaSinPrecioException("No se pueden dejar zonas sin precio"));
                            }

                            if (hayZonasInvalidas(zonas, porZona)) {
                                return Flux.error(new ZonaSinPrecioException("Existen zonas que no pertenecen al recinto del evento"));
                            }

                            return precioZonaRepositoryPort.eliminarPorEvento(eventoId)
                                    .thenMany(Flux.fromIterable(porZona.values())
                                            .map(precio -> buildPrecioZona(precio, eventoId))
                                            .flatMap(precioZonaRepositoryPort::guardar));
                        }));
    }

    private Map<UUID, PrecioZona> obtenerPreciosPorZona(List<PrecioZona> precios) {
        return precios.stream()
                .peek(PrecioZona::validarDatosRegistro)
                .collect(Collectors.toMap(PrecioZona::getZonaId, Function.identity(), (a, b) -> b));
    }

    private boolean hayPreciosIncompletos(List<Zona> zonas, Map<UUID, PrecioZona> preciosPorZona){
        return zonas.stream().anyMatch(zona -> !preciosPorZona.containsKey(zona.getId()));
    }

    private boolean hayZonasInvalidas(List<Zona> zonas, Map<UUID, PrecioZona> preciosPorZona){
        return preciosPorZona.keySet().stream()
                .anyMatch(zonaId -> zonas.stream().noneMatch(zona -> zona.getId().equals(zonaId)));
    }

    private PrecioZona buildPrecioZona(PrecioZona precio, UUID eventoId){
        return precio.toBuilder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .build();
    }
}

