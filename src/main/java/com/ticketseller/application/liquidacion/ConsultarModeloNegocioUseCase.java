package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.LiquidacionNoConfiguradaException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.ConfiguracionLiquidacion;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarModeloNegocioUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<ConfiguracionLiquidacion> ejecutar(UUID recintoId) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado con id: " + recintoId)))
                .flatMap(recinto -> recintoRepositoryPort.buscarConfiguracionLiquidacion(recintoId)
                        .switchIfEmpty(Mono.error(new LiquidacionNoConfiguradaException(
                                "El recinto no tiene modelo de negocio configurado"))));
    }
}
