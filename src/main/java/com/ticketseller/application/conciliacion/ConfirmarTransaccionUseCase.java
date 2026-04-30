package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.conciliacion.PagoEnDiscrepanciaException;
import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class ConfirmarTransaccionUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;

    public Mono<Pago> ejecutar(UUID ventaId, String idExternoPasarela, BigDecimal montoPasarela) {
        return pagoRepositoryPort.buscarPorIdExterno(idExternoPasarela)
                .flatMap(this::procesarYConfirmarPago)
                .switchIfEmpty(Mono.defer(() -> crearYConfirmar(ventaId, idExternoPasarela, montoPasarela)));
    }

    private Mono<Pago> procesarYConfirmarPago(Pago pago) {
        if (pagoConfirmado(pago))
            return Mono.just(pago);
        return confirmarPagoExistente(pago);
    }

    private boolean pagoConfirmado(Pago pago) {
        return EstadoConciliacion.CONFIRMADO.equals(pago.getEstado());
    }

    private Mono<Pago> confirmarPagoExistente(Pago pago) {
        if (pagoEnDiscrepancia(pago)) {
            return Mono.error(new PagoEnDiscrepanciaException(pago.getId()));
        }
        return ventaRepositoryPort.actualizarEstado(pago.getVentaId(), EstadoVenta.COMPLETADA)
                .then(pagoRepositoryPort.actualizarEstado(pago.getId(), EstadoConciliacion.CONFIRMADO));
    }

    private boolean pagoEnDiscrepancia(Pago pago) {
        return EstadoConciliacion.EN_DISCREPANCIA.equals(pago.getEstado());
    }

    private Mono<Pago> crearYConfirmar(UUID ventaId, String idExterno, BigDecimal montoPasarela) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNoEncontradaException(ventaId)))
                .flatMap(venta -> procesarVenta(venta, montoPasarela, idExterno));
    }

    private Mono<Pago> procesarVenta(Venta venta, BigDecimal montoPasarela, String idExternoPasarela){
        EstadoConciliacion estadoInicial = estadoVenta(venta, montoPasarela);

        if (ventaEnDiscrepancia(estadoInicial)) {
            Pago pagoDiscrepancia = buildPago(venta, idExternoPasarela, EstadoConciliacion.EN_DISCREPANCIA, montoPasarela);
            return pagoRepositoryPort.guardar(pagoDiscrepancia)
                    .flatMap(p -> Mono.error(new PagoEnDiscrepanciaException(p.getId())));
        }

        Pago pagoConfirmado = buildPago(venta, idExternoPasarela, EstadoConciliacion.CONFIRMADO, montoPasarela);
        return ventaRepositoryPort.actualizarEstado(venta.getId(), EstadoVenta.COMPLETADA)
                .then(pagoRepositoryPort.guardar(pagoConfirmado));
    }

    private EstadoConciliacion estadoVenta(Venta venta, BigDecimal montoPasarela){
        return venta.getTotal().compareTo(montoPasarela) == 0
                ? EstadoConciliacion.VERIFICADO
                : EstadoConciliacion.EN_DISCREPANCIA;
    }

    private boolean ventaEnDiscrepancia(EstadoConciliacion estadoVenta){
        return EstadoConciliacion.EN_DISCREPANCIA.equals(estadoVenta);
    }

    private Pago buildPago(Venta venta, String idExternoPasarela, EstadoConciliacion estado, BigDecimal montoPasarela) {
        return Pago.builder()
                .id(UUID.randomUUID())
                .ventaId(venta.getId())
                .idExternoPasarela(idExternoPasarela)
                .montoEsperado(venta.getTotal())
                .montoPasarela(montoPasarela)
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }
}
