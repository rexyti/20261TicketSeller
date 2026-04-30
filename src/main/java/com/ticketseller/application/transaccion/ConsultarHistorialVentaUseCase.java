package com.ticketseller.application.transaccion;

import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.repository.HistorialEstadoVentaRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarHistorialVentaUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final HistorialEstadoVentaRepositoryPort historialRepositoryPort;

    public Flux<HistorialEstadoVenta> ejecutar(UUID ventaId) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNoEncontradaException(ventaId)))
                .flatMapMany(venta -> historialRepositoryPort.buscarPorVentaId(ventaId));
    }
}
