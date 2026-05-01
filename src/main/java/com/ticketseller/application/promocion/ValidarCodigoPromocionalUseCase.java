package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.CodigoPromoAgotadoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoExpiradoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoInvalidoException;
import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ValidarCodigoPromocionalUseCase {

    private final CodigoPromocionalRepositoryPort codigoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;
    private final DescuentoRepositoryPort descuentoRepositoryPort;

    public Mono<Descuento> ejecutar(String codigo) {
        return codigoRepositoryPort.buscarPorCodigo(codigo)
                .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("Código promocional inválido")))
                .flatMap(codigoPromo -> {
                    if (!codigoPromo.estaVigente(LocalDateTime.now())) {
                        return Mono.error(new CodigoPromoExpiradoException("CÓDIGO EXPIRADO"));
                    }
                    if (!codigoPromo.tieneUsosDisponibles()) {
                        return Mono.error(new CodigoPromoAgotadoException("CÓDIGO YA UTILIZADO"));
                    }
                    return Mono.just(codigoPromo);
                })
                .flatMap(codigoPromo -> promocionRepositoryPort.buscarPorId(codigoPromo.getPromocionId())
                        .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("Código promocional inválido")))
                        .flatMap(promocion -> {
                            if (!promocion.estaActiva()) {
                                return Mono.error(new PromocionNoActivaException("La promoción asociada no está activa"));
                            }
                            return codigoRepositoryPort.incrementarUsos(codigoPromo.getId());
                        }))
                .flatMap(codigoActualizado -> descuentoRepositoryPort
                        .buscarPorPromocionId(codigoActualizado.getPromocionId())
                        .next()
                        .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("El código no tiene descuento asociado"))));
    }
}
