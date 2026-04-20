package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.port.out.CompuertaRepositoryPort;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearCompuertaUseCase {

    private final CompuertaRepositoryPort compuertaRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Mono<Compuerta> ejecutar(UUID recintoId, Compuerta request) {
        return Mono.justOrEmpty(request)
                .switchIfEmpty(Mono.error(new CompuertaInvalidaException("La compuerta es obligatoria")))
                .map(Compuerta::normalizarDatosRegistro)
                .doOnNext(Compuerta::validarDatosRegistro)
                .flatMap(compuertaValida -> recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarZonaYGuardar(recintoId, compuertaValida)));
    }

    private Mono<Compuerta> validarZonaYGuardar(UUID recintoId, Compuerta request) {
        if (compuertaSinZona(request)) {
            return compuertaRepositoryPort.guardar(buildCompuerta(recintoId, request, true));
        }
        return zonaRepositoryPort.buscarPorId(request.getZonaId())
                .switchIfEmpty(Mono.error(new ZonaCapacidadExcedidaException("La zona indicada no existe")))
                .flatMap(zona -> compuertaRepositoryPort.guardar(buildCompuerta(recintoId, request, false)));
    }

    private boolean compuertaSinZona(Compuerta request) {
        return request.getZonaId() == null;
    }

    private Compuerta buildCompuerta(UUID recintoId, Compuerta request, boolean esGeneral) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .esGeneral(esGeneral)
                .build();
    }
}

