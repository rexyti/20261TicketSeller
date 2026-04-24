package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.model.TipoAsiento;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarTiposAsientoUseCase {
    private final TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    public Flux<TipoAsiento> ejecutar(String estadoFiltro) {
        return tipoAsientoRepositoryPort.listarTodos()
                .filter(tipo -> estadoFiltro == null || tipo.getEstado().name().equalsIgnoreCase(estadoFiltro));
    }

    public Flux<Boolean> calcularEnUso(TipoAsiento tipo) {
        return tipoAsientoRepositoryPort.tieneAsignacionEnZona(tipo.getId()).flux();
    }
}
