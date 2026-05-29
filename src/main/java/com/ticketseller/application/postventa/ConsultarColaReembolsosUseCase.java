package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class ConsultarColaReembolsosUseCase {
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;

    public Flux<Reembolso> ejecutar() {
        return reembolsoRepositoryPort.buscarPorEstado(EstadoReembolso.PENDIENTE);
    }
}
