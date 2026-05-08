package com.ticketseller.application.compuerta;

import com.ticketseller.domain.exception.compuerta.CompuertaInvalidaException;
import com.ticketseller.domain.exception.compuerta.CompuertaLimiteExcedidoException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
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
                .switchIfEmpty(Mono.error(new ZonaNotFoundException("La zona indicada no existe")))
                .flatMap(zona -> validarLimiteCompuertas(recintoId)
                        .then(Mono.defer(() -> compuertaRepositoryPort.guardar(buildCompuerta(recintoId, request, false)))));
    }

    private Mono<Void> validarLimiteCompuertas(UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado: " + recintoId)))
                .flatMap(recinto -> compuertaRepositoryPort.buscarPorRecintoId(recintoId)
                        .filter(this::compuertaConZona)
                        .count()
                        .flatMap(asignadas -> cantidadMaximaCompuertasNoSuperada(asignadas, recinto)
                            ? Mono.empty()
                            : Mono.error(new CompuertaLimiteExcedidoException(
                            "El recinto ya tiene %d compuertas asignadas, límite máximo: %d"
                            .formatted(asignadas, recinto.getCompuertasIngreso())))
                        ));

    }

    private boolean compuertaConZona(Compuerta compuerta){
        return compuerta.getZonaId() != null;
    }

    private boolean cantidadMaximaCompuertasNoSuperada(Long compuertas, Recinto recinto){
        return compuertas < recinto.getCompuertasIngreso();
    }

    private boolean compuertaSinZona(Compuerta request) {
        return request.getZonaId() == null;
    }

    private Compuerta buildCompuerta(UUID recintoId, Compuerta request, boolean esGeneral) {
        return request.toBuilder()
                .recintoId(recintoId)
                .esGeneral(esGeneral)
                .build();
    }
}

