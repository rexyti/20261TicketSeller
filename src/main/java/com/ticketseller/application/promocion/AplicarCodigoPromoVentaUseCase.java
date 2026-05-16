package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarCodigoPromoVentaUseCase {

    private final ValidarCodigoPromocionalUseCase validarCodigoPromocionalUseCase;
    private final VentaRepositoryPort ventaRepositoryPort;

    public Mono<DescuentoAplicado> ejecutar(UUID ventaId, String codigo) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> validarCodigoPromocionalUseCase.ejecutar(codigo)
                        .flatMap(descuento -> aplicarDescuento(venta, descuento)));
    }

    private Mono<DescuentoAplicado> aplicarDescuento(Venta venta, Descuento descuento) {
        BigDecimal subtotal = venta.getTotal();
        BigDecimal montoDescuento = calcularMonto(descuento, venta.getZonaId(), subtotal);
        BigDecimal totalFinal = subtotal.subtract(montoDescuento).max(BigDecimal.ZERO);
        return ventaRepositoryPort.actualizarTotal(venta.getId(), totalFinal)
                .thenReturn(new DescuentoAplicado(subtotal, montoDescuento, totalFinal));
    }

    private BigDecimal calcularMonto(Descuento descuento, UUID zonaId, BigDecimal subtotal) {
        if (!descuento.aplicaAZona(zonaId)) {
            return BigDecimal.ZERO;
        }
        if (TipoDescuento.PORCENTAJE.equals(descuento.getTipo())) {
            return subtotal.multiply(descuento.getValor())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        return descuento.getValor().min(subtotal);
    }
}
