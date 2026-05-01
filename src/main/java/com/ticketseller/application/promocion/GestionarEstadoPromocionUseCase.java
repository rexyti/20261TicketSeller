package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.exception.promocion.TransicionPromocionInvalidaException;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class GestionarEstadoPromocionUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;

    public Mono<Promocion> ejecutar(UUID id, EstadoPromocion nuevoEstado) {
        return promocionRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new PromocionNotFoundException("La promoción indicada no existe")))
                .flatMap(promocion -> validarTransicion(promocion, nuevoEstado))
                .map(promocion -> promocion.toBuilder().estado(nuevoEstado).build())
                .flatMap(promocionRepositoryPort::guardar);
    }

    private Mono<Promocion> validarTransicion(Promocion promocion, EstadoPromocion nuevoEstado) {
        EstadoPromocion actual = promocion.getEstado();

        if (EstadoPromocion.FINALIZADA.equals(actual)) {
            return Mono.error(new TransicionPromocionInvalidaException(
                    "Una promoción finalizada no puede cambiar de estado"));
        }
        if (EstadoPromocion.ACTIVA.equals(nuevoEstado) && EstadoPromocion.PAUSADA.equals(actual)) {
            return Mono.just(promocion);
        }
        if (EstadoPromocion.PAUSADA.equals(nuevoEstado) && EstadoPromocion.ACTIVA.equals(actual)) {
            return Mono.just(promocion);
        }
        if (EstadoPromocion.FINALIZADA.equals(nuevoEstado)) {
            return Mono.just(promocion);
        }
        return Mono.error(new TransicionPromocionInvalidaException(
                "Transición de estado inválida: " + actual + " → " + nuevoEstado));
    }
}
