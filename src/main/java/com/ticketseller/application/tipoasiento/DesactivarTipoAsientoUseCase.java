package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.exception.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.TipoAsientoNotFoundException;
import com.ticketseller.domain.model.EstadoTipoAsiento;
import com.ticketseller.domain.model.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class DesactivarTipoAsientoUseCase {

    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Mono<TipoAsiento> ejecutar(UUID id) {
        return tipoAsientoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new TipoAsientoNotFoundException("Tipo de asiento no encontrado")))
                .flatMap(tipo -> tipoAsientoRepositoryPort.tieneEventosFuturos(tipo.getId())
                        .doOnNext(this::validarUsoEnEventos)
                        .thenReturn(tipo)
                )
                .flatMap(this::desactivarYGuardar);
    }

    private void validarUsoEnEventos(boolean tieneEventos) {
        if (tieneEventos) {
            throw new TipoAsientoEnUsoException("No se puede desactivar el tipo de asiento porque está siendo utilizado en secciones con eventos programados.");
        }
    }

    private Mono<TipoAsiento> desactivarYGuardar(TipoAsiento tipo) {
        TipoAsiento desactivado = tipo.toBuilder()
                .estado(EstadoTipoAsiento.INACTIVO)
                .build();
        return tipoAsientoRepositoryPort.guardar(desactivado);
    }
}
