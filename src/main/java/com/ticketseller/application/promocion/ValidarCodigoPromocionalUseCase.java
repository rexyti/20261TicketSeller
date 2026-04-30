package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.CodigoPromoAgotadoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoExpiradoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoInvalidoException;
import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class ValidarCodigoPromocionalUseCase {

    private final CodigoPromocionalRepositoryPort codigoPromocionalRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;
    private final DescuentoRepositoryPort descuentoRepositoryPort;

    public Mono<Descuento> ejecutar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Mono.error(new CodigoPromoInvalidoException("CODIGO INVALIDO"));
        }
        LocalDateTime ahora = LocalDateTime.now();
        return codigoPromocionalRepositoryPort.buscarPorCodigo(codigo.trim())
                .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("CODIGO INVALIDO")))
                .flatMap(codigoPromocional -> validarContexto(codigoPromocional, ahora))
                .flatMap(codigoPromocional -> codigoPromocionalRepositoryPort.incrementarUsoAtomico(codigoPromocional.getCodigo(), ahora)
                        .flatMap(actualizado -> actualizado
                                ? Mono.just(codigoPromocional)
                                : determinarFallo(codigoPromocional, ahora)))
                .flatMap(codigoPromocional -> descuentoRepositoryPort.buscarPorPromocionId(codigoPromocional.getPromocionId())
                        .next()
                        .switchIfEmpty(Mono.error(new IllegalStateException("La promocion no tiene un descuento configurado"))));
    }

    private Mono<CodigoPromocional> validarContexto(CodigoPromocional codigo, LocalDateTime ahora) {
        if (codigo.getFechaFin().isBefore(ahora) || EstadoCodigoPromocional.EXPIRADO.equals(codigo.getEstado())) {
            return Mono.error(new CodigoPromoExpiradoException("CODIGO EXPIRADO"));
        }
        if (EstadoCodigoPromocional.AGOTADO.equals(codigo.getEstado())
                || (codigo.getUsosMaximos() != null && codigo.getUsosActuales() >= codigo.getUsosMaximos())) {
            return Mono.error(new CodigoPromoAgotadoException("CODIGO YA UTILIZADO"));
        }
        return promocionRepositoryPort.buscarPorId(codigo.getPromocionId())
                .switchIfEmpty(Mono.error(new CodigoPromoInvalidoException("CODIGO INVALIDO")))
                .filter(promocion -> EstadoPromocion.ACTIVA.equals(promocion.getEstado()))
                .switchIfEmpty(Mono.error(new PromocionNoActivaException("La promocion asociada no esta activa")))
                .thenReturn(codigo);
    }

    private Mono<CodigoPromocional> determinarFallo(CodigoPromocional codigo, LocalDateTime ahora) {
        if (codigo.getFechaFin().isBefore(ahora)) {
            return Mono.error(new CodigoPromoExpiradoException("CODIGO EXPIRADO"));
        }
        return Mono.error(new CodigoPromoAgotadoException("CODIGO YA UTILIZADO"));
    }
}

