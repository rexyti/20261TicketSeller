package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.asiento.TipoAsientoNotFoundException;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarTipoAsientoUseCase {

    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<TipoAsiento> ejecutar(UUID id){
        return tipoAsientoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new TipoAsientoNotFoundException("Tipo de asiento no encontrado")));
    }
}
