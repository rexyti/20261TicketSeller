package com.ticketseller.application.transaccion;

import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarTransaccionesUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;

    public Flux<Venta> ejecutar(FiltroTransacciones filtro) {
        return ventaRepositoryPort.buscarConFiltros(
                filtro.estado(),
                filtro.fechaInicio(),
                filtro.fechaFin(),
                filtro.eventoId()
        );
    }
}
