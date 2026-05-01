package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearDescuentoUseCase {

    private final DescuentoRepositoryPort descuentoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;

    public Mono<Descuento> ejecutar(UUID promocionId, Descuento request) {
        return promocionRepositoryPort.buscarPorId(promocionId)
                .switchIfEmpty(Mono.error(new PromocionNotFoundException("La promoción indicada no existe")))
                .flatMap(promocion -> {
                    if (!promocion.estaActiva()) {
                        return Mono.error(new PromocionNoActivaException("Solo se pueden agregar descuentos a promociones activas"));
                    }
                    return Mono.just(promocion);
                })
                .doOnNext(p -> validarValor(request))
                .flatMap(p -> validarZona(request, p.getEventoId()))
                .map(validado -> buildNuevoDescuento(promocionId, request))
                .flatMap(descuentoRepositoryPort::guardar);
    }

    private void validarValor(Descuento descuento) {
        if (descuento.getValor() == null || descuento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del descuento debe ser mayor a cero");
        }
        if (TipoDescuento.PORCENTAJE.equals(descuento.getTipo())
                && descuento.getValor().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de descuento no puede superar el 100%");
        }
    }

    private Mono<Descuento> validarZona(Descuento descuento, UUID eventoId) {
        if (descuento.getZonaId() == null) {
            return Mono.just(descuento);
        }
        return zonaRepositoryPort.buscarPorId(descuento.getZonaId())
                .switchIfEmpty(Mono.error(new ZonaNotFoundException("La zona indicada no existe")))
                .thenReturn(descuento);
    }

    private Descuento buildNuevoDescuento(UUID promocionId, Descuento request) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .promocionId(promocionId)
                .build();
    }
}
