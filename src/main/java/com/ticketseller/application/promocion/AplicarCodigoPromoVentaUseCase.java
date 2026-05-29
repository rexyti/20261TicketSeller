package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
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
    private final CodigoPromocionalRepositoryPort codigoRepositoryPort;

    public Mono<DescuentoAplicado> ejecutar(UUID ventaId, String codigo) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> validarCodigoPromocionalUseCase.ejecutar(codigo)
                        .flatMap(codigoValidado -> aplicarDescuento(venta.getId(), venta.getTotal(),
                                venta.getZonaId(), codigoValidado)));
    }

    private Mono<DescuentoAplicado> aplicarDescuento(UUID ventaId, BigDecimal subtotalOriginal,
                                                      UUID zonaId, CodigoValidado codigoValidado) {
        BigDecimal montoDescuento = calcularMontoDescuento(subtotalOriginal, codigoValidado.descuento(), zonaId);
        BigDecimal totalFinal = subtotalOriginal.subtract(montoDescuento).max(BigDecimal.ZERO);

        return ventaRepositoryPort.actualizarTotal(ventaId, totalFinal)
                .then(codigoRepositoryPort.incrementarUsos(codigoValidado.codigoId()))
                .thenReturn(new DescuentoAplicado(subtotalOriginal, montoDescuento, totalFinal));
    }

    private BigDecimal calcularMontoDescuento(BigDecimal subtotal, Descuento descuento, UUID zonaId) {
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
