package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarCodigosPromocionalesUseCase {

    private final CodigoPromocionalRepositoryPort codigoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;

    public Flux<CodigoPromocional> ejecutar(UUID promocionId) {
        return promocionRepositoryPort.buscarPorId(promocionId)
                .switchIfEmpty(Mono.error(new PromocionNotFoundException("La promoción indicada no existe")))
                .thenMany(codigoRepositoryPort.buscarPorPromocionId(promocionId));
    }
}
