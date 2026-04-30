package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ExpirarTransaccionesPendientesUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final VentaRepositoryPort ventaRepositoryPort;

    public Flux<Pago> ejecutar(LocalDateTime fechaCorte) {
        return pagoRepositoryPort.buscarPendientesAnterioresA(fechaCorte)
                .flatMap(this::expirarPago);
    }

    private Mono<Pago> expirarPago(Pago pago) {
        return ventaRepositoryPort.actualizarEstado(pago.getVentaId(), EstadoVenta.EXPIRADA)
                .then(pagoRepositoryPort.actualizarEstado(pago.getId(), EstadoConciliacion.EXPIRADO));
    }
}
