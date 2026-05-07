package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarDescuentosUseCase {

    private final DescuentoRepositoryPort descuentoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;

    public Flux<Descuento> ejecutar(UUID promocionId) {
        return promocionRepositoryPort.buscarPorId(promocionId)
                .switchIfEmpty(Mono.error(new PromocionNotFoundException("La promoción indicada no existe")))
                .thenMany(descuentoRepositoryPort.buscarPorPromocionId(promocionId));
    }
}
