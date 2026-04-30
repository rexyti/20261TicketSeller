package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ListarDiscrepanciaUseCase {
    private final PagoRepositoryPort pagoRepositoryPort;

    public Flux<Pago> ejecutar() {
        return pagoRepositoryPort.buscarPorEstado(EstadoConciliacion.EN_DISCREPANCIA);
    }
}
