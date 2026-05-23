package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarCodigoPromoVentaUseCase {

    private final ValidarCodigoPromocionalUseCase validarCodigoPromocionalUseCase;
    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;
    private final CodigoPromocionalRepositoryPort codigoRepositoryPort;

    public Mono<DescuentoAplicado> ejecutar(UUID ventaId, String codigo) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> validarCodigoPromocionalUseCase.ejecutar(codigo)
                        .flatMap(codigoValidado -> aplicarDescuento(venta, codigoValidado)));
    }

    private Mono<DescuentoAplicado> aplicarDescuento(Venta venta, CodigoValidado codigoValidado) {
        return ticketRepositoryPort.buscarPorVenta(venta.getId())
                .collectList()
                .flatMap(tickets -> {
                    BigDecimal subtotal = sumarPrecios(tickets);
                    List<Ticket> actualizados = distribuirDescuento(tickets, codigoValidado.descuento());
                    BigDecimal totalFinal = sumarPrecios(actualizados);
                    BigDecimal montoDescuento = subtotal.subtract(totalFinal).max(BigDecimal.ZERO);
                    return ticketRepositoryPort.guardarTodos(actualizados)
                            .then(ventaRepositoryPort.actualizarTotal(venta.getId(), totalFinal))
                            .then(codigoRepositoryPort.incrementarUsos(codigoValidado.codigoId()))
                            .thenReturn(new DescuentoAplicado(subtotal, montoDescuento, totalFinal));
                });
    }

    private BigDecimal sumarPrecios(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Ticket> distribuirDescuento(List<Ticket> tickets, Descuento descuento) {
        List<Ticket> aplicables = tickets.stream()
                .filter(t -> descuento.aplicaAZona(t.getZonaId()))
                .toList();
        if (aplicables.isEmpty()) {
            return tickets;
        }
        if (TipoDescuento.PORCENTAJE.equals(descuento.getTipo())) {
            return aplicarPorcentaje(tickets, descuento);
        }
        return distribuirMontoFijo(tickets, aplicables, descuento.getValor());
    }

    private List<Ticket> aplicarPorcentaje(List<Ticket> tickets, Descuento descuento) {
        return tickets.stream()
                .map(t -> {
                    if (!descuento.aplicaAZona(t.getZonaId())) {
                        return t;
                    }
                    BigDecimal descuentoUnitario = t.getPrecio().multiply(descuento.getValor())
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    return t.toBuilder()
                            .precio(t.getPrecio().subtract(descuentoUnitario).max(BigDecimal.ZERO))
                            .build();
                })
                .toList();
    }

    private List<Ticket> distribuirMontoFijo(List<Ticket> tickets, List<Ticket> aplicables, BigDecimal montoFijo) {
        BigDecimal subtotalAplicable = sumarPrecios(aplicables);
        BigDecimal descuentoTotal = montoFijo.min(subtotalAplicable);
        Map<UUID, BigDecimal> descuentosPorId = calcularDescuentosProporcionales(
                aplicables, subtotalAplicable, descuentoTotal);
        return tickets.stream()
                .map(t -> {
                    BigDecimal share = descuentosPorId.get(t.getId());
                    return share != null
                            ? t.toBuilder().precio(t.getPrecio().subtract(share).max(BigDecimal.ZERO)).build()
                            : t;
                })
                .toList();
    }

    private Map<UUID, BigDecimal> calcularDescuentosProporcionales(List<Ticket> tickets,
                                                                    BigDecimal subtotal,
                                                                    BigDecimal descuentoTotal) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < tickets.size() - 1; i++) {
            Ticket t = tickets.get(i);
            BigDecimal share = descuentoTotal.multiply(t.getPrecio())
                    .divide(subtotal, 2, RoundingMode.HALF_UP);
            result.put(t.getId(), share);
            acumulado = acumulado.add(share);
        }
        Ticket last = tickets.get(tickets.size() - 1);
        result.put(last.getId(), descuentoTotal.subtract(acumulado));
        return result;
    }
}
