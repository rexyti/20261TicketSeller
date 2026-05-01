package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.CodigoPromoAgotadoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoExpiradoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoInvalidoException;
import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.Promocion;
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
                .flatMap(this::validarVigencia)
                .flatMap(this::validarYRegistrarUso)
                .flatMap(this::obtenerDescuento);
    }

    private Mono<CodigoPromocional> validarVigencia(CodigoPromocional codigoPromo) {
        return Mono.just(codigoPromo)
                .filter(c -> c.estaVigente(LocalDateTime.now()))
                .switchIfEmpty(Mono.error(new CodigoPromoExpiradoException("CÓDIGO EXPIRADO")))
                .filter(CodigoPromocional::tieneUsosDisponibles)
                .switchIfEmpty(Mono.error(new CodigoPromoAgotadoException("CÓDIGO YA UTILIZADO")));
    }

    private Mono<CodigoPromocional> validarYRegistrarUso(CodigoPromocional codigoPromo) {
        return promocionRepositoryPort.buscarPorId(codigoPromo.getPromocionId())
                .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("Código promocional inválido")))
                .filter(Promocion::estaActiva)
                .switchIfEmpty(Mono.error(new PromocionNoActivaException("La promoción asociada no está activa")))
                .flatMap(p -> codigoRepositoryPort.incrementarUsos(codigoPromo.getId()));
    }

    private Mono<Descuento> obtenerDescuento(CodigoPromocional codigoActualizado) {
        return descuentoRepositoryPort
                .buscarPorPromocionId(codigoActualizado.getPromocionId())
                .next()
                .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("El código no tiene descuento asociado")));
    }
}
