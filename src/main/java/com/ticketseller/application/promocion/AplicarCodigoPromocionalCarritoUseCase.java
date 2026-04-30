package com.ticketseller.application.promocion;

import com.ticketseller.application.checkout.VentaDetalle;
import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarCodigoPromocionalCarritoUseCase {

    private final ValidarCodigoPromocionalUseCase validarCodigoPromocionalUseCase;
    private final AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase;
    private final VentaRepositoryPort ventaRepositoryPort;
    private final TicketRepositoryPort ticketRepositoryPort;

    public Mono<AplicacionDescuentoResultado> ejecutar(UUID ventaId, String codigo) {
        Mono<VentaDetalle> ventaDetalleMono = ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNotFoundException("Venta no encontrada")))
                .flatMap(venta -> ticketRepositoryPort.buscarPorVenta(venta.getId())
                        .collectList()
                        .map(tickets -> new VentaDetalle(venta, tickets)));
        Mono<Descuento> descuentoMono = validarCodigoPromocionalUseCase.ejecutar(codigo);

        return Mono.zip(ventaDetalleMono, descuentoMono)
                .map(tuple -> {
                    VentaDetalle detalle = tuple.getT1();
                    BigDecimal subtotal = detalle.tickets().stream()
                            .map(ticket -> ticket.getPrecio())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    AplicacionDescuentoResultado resultado = aplicarDescuentoCarritoUseCase.aplicarConCodigo(
                            subtotal, detalle.tickets(), tuple.getT2(), codigo
                    );
                    Venta actualizada = detalle.venta().toBuilder().total(resultado.totalFinal()).build();
                    return new AplicacionCodigoContexto(actualizada, resultado);
                })
                .flatMap(contexto -> ventaRepositoryPort.guardar(contexto.venta())
                        .thenReturn(contexto.resultado()));
    }

    private record AplicacionCodigoContexto(Venta venta, AplicacionDescuentoResultado resultado) {
    }
}

