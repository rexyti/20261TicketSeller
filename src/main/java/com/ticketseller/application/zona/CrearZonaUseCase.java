package com.ticketseller.application.zona;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
import com.ticketseller.domain.exception.zona.ZonaNombreDuplicadoException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearZonaUseCase {

    private final ZonaRepositoryPort zonaRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Zona> ejecutar(UUID recintoId, Zona zona) {
        return Mono.justOrEmpty(zona)
                .switchIfEmpty(Mono.error(new ZonaInvalidaException("La zona es obligatoria")))
                .map(Zona::normalizarDatosRegistro)
                .doOnNext(Zona::validarDatosRegistro)
                .flatMap(zonaValida -> recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarYCrear(recinto, zonaValida)));
    }

    private Mono<Zona> validarYCrear(Recinto recinto, Zona zona) {
        return validarNombreUnico(recinto.getId(), zona.getNombre())
                .then(validarCapacidadDisponible(recinto, zona))
                .then(Mono.defer(() -> zonaRepositoryPort.guardar(buildZona(recinto.getId(), zona))));
    }

    private Mono<Void> validarNombreUnico(UUID recintoId, String nombreZona) {
        return zonaRepositoryPort.buscarPorRecintoId(recintoId)
                .filter(existing -> existing.getNombre().equalsIgnoreCase(nombreZona))
                .hasElements()
                .filter(existeNombre -> !existeNombre)
                .switchIfEmpty(Mono.error(new ZonaNombreDuplicadoException(
                        "Ya existe una zona con ese nombre en el recinto"
                )))
                .then();
    }

    private Mono<Void> validarCapacidadDisponible(Recinto recinto, Zona zona) {
        return zonaRepositoryPort.sumarCapacidadesPorRecinto(recinto.getId())
                .filter(suma -> !capacidadSuperada(suma + zona.getCapacidad(), recinto.getCapacidadMaxima()))
                .switchIfEmpty(Mono.error(new ZonaCapacidadExcedidaException(
                        "La capacidad de zonas excede la capacidad total del recinto"
                )))
                .then();
    }

    private boolean capacidadSuperada(int capacidadUsada, int capacidadMaxima) {
        return capacidadUsada > capacidadMaxima;
    }

    private Zona buildZona(UUID recintoId, Zona request) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .build();
    }
}

