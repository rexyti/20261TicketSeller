package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.UsuarioNoAutorizadoParaPreventaException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarDescuentoCarritoUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;
    private final DescuentoRepositoryPort descuentoRepositoryPort;

    public Mono<DescuentoAplicado> ejecutar(UUID eventoId, TipoUsuario tipoUsuario, List<ItemCarrito> items) {
        LocalDateTime ahora = LocalDateTime.now();
        return validarAccesoPreventa(eventoId, tipoUsuario, ahora)
                .then(calcularDescuento(eventoId, items, ahora));
    }

    private Mono<Void> validarAccesoPreventa(UUID eventoId, TipoUsuario tipoUsuario, LocalDateTime ahora) {
        return promocionRepositoryPort.buscarActivasPorEvento(eventoId)
                .filter(p -> TipoPromocion.PREVENTA.equals(p.getTipo()))
                .filter(p -> p.getTipoUsuarioRestringido() != null)
                .filter(p -> p.estaVigenteEn(ahora))
                .collectList()
                .flatMap(preventas -> {
                    if (preventas.isEmpty()) {
                        return Mono.<Void>empty();
                    }
                    boolean autorizado = preventas.stream()
                            .anyMatch(p -> p.getTipoUsuarioRestringido().equals(tipoUsuario));
                    if (!autorizado) {
                        return Mono.error(new UsuarioNoAutorizadoParaPreventaException(
                                "El usuario no está autorizado para acceder a esta preventa"));
                    }
                    return Mono.<Void>empty();
                });
    }

    private Mono<DescuentoAplicado> calcularDescuento(UUID eventoId, List<ItemCarrito> items, LocalDateTime ahora) {
        BigDecimal subtotal = items.stream()
                .map(ItemCarrito::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return descuentoRepositoryPort.buscarActivosPorEvento(eventoId, ahora)
                .collectList()
                .map(descuentos -> aplicarDescuentos(subtotal, items, descuentos));
    }

    private DescuentoAplicado aplicarDescuentos(BigDecimal subtotal, List<ItemCarrito> items,
                                                List<Descuento> descuentos) {
        BigDecimal montoDescuento = descuentos.stream()
                .map(d -> calcularMontoDescuento(d, items))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFinal = subtotal.subtract(montoDescuento).max(BigDecimal.ZERO);
        return new DescuentoAplicado(subtotal, montoDescuento, totalFinal);
    }

    private BigDecimal calcularMontoDescuento(Descuento descuento, List<ItemCarrito> items) {
        BigDecimal base = items.stream()
                .filter(item -> descuento.aplicaAZona(item.zonaId()))
                .map(ItemCarrito::precio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (TipoDescuento.PORCENTAJE.equals(descuento.getTipo())) {
            return base.multiply(descuento.getValor())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return descuento.getValor().min(base);
    }
}
