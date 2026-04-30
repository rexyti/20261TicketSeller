package com.ticketseller.application.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class GestionarEstadoPromocionUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;

    public Mono<Promocion> ejecutar(UUID promocionId, EstadoPromocion nuevoEstado) {
        if (promocionId == null || nuevoEstado == null) {
            return Mono.error(new IllegalArgumentException("promocionId y nuevoEstado son obligatorios"));
        }
        return promocionRepositoryPort.buscarPorId(promocionId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Promocion no encontrada")))
                .flatMap(actual -> validarTransicion(actual.getEstado(), nuevoEstado))
                .flatMap(estado -> promocionRepositoryPort.actualizarEstado(promocionId, estado))
                .switchIfEmpty(Mono.error(new IllegalStateException("No fue posible actualizar el estado de la promocion")));
    }

    private Mono<EstadoPromocion> validarTransicion(EstadoPromocion actual, EstadoPromocion nuevo) {
        if (EstadoPromocion.FINALIZADA.equals(actual)) {
            return Mono.error(new IllegalArgumentException("Una promocion finalizada no puede reactivarse"));
        }
        boolean permitida = (EstadoPromocion.ACTIVA.equals(actual) && EstadoPromocion.PAUSADA.equals(nuevo))
                || (EstadoPromocion.PAUSADA.equals(actual) && EstadoPromocion.ACTIVA.equals(nuevo))
                || EstadoPromocion.FINALIZADA.equals(nuevo)
                || actual.equals(nuevo);
        if (!permitida) {
            return Mono.error(new IllegalArgumentException("Transicion de estado no permitida"));
        }
        return Mono.just(nuevo);
    }
}

