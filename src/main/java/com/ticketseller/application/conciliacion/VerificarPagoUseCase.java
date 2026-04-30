package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class VerificarPagoUseCase {

    private final VentaRepositoryPort ventaRepositoryPort;
    private final PagoRepositoryPort pagoRepositoryPort;

    public Mono<Pago> ejecutar(UUID ventaId, BigDecimal montoPasarela, String idExternoPasarela) {
        if (idExternoValido(idExternoPasarela)) {
            return pagoRepositoryPort.buscarPorIdExterno(idExternoPasarela)
                    .switchIfEmpty(Mono.defer(() -> crearPago(ventaId, montoPasarela, idExternoPasarela)));
        }
        return crearPago(ventaId, montoPasarela, null);
    }

    private boolean idExternoValido(String idExterno) {
        return idExterno != null && !idExterno.isBlank();
    }

    private Mono<Pago> crearPago(UUID ventaId, BigDecimal montoPasarela, String idExterno) {
        return ventaRepositoryPort.buscarPorId(ventaId)
                .switchIfEmpty(Mono.error(new VentaNoEncontradaException(ventaId)))
                .flatMap(venta -> {
                    EstadoConciliacion estado = determinarEstado(venta.getTotal(), montoPasarela);
                    Pago pago = buildPago(venta, idExterno, estado, montoPasarela);
                    return pagoRepositoryPort.guardar(pago);
                });
    }

    private Pago buildPago(Venta venta, String idExterno, EstadoConciliacion estado, BigDecimal montoPasarela) {
        return Pago.builder()
                .id(UUID.randomUUID())
                .ventaId(venta.getId())
                .idExternoPasarela(idExterno)
                .montoEsperado(venta.getTotal())
                .montoPasarela(montoPasarela)
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }

    private EstadoConciliacion determinarEstado(BigDecimal montoEsperado, BigDecimal montoPasarela) {
        if (noHayMontoPasarela(montoPasarela)) {
            return EstadoConciliacion.PENDIENTE;
        }
        return montoCoincide(montoEsperado, montoPasarela)
                ? EstadoConciliacion.VERIFICADO
                : EstadoConciliacion.EN_DISCREPANCIA;
    }

    private boolean noHayMontoPasarela(BigDecimal montoPasarela) {
        return montoPasarela == null;
    }

    private boolean montoCoincide(BigDecimal montoEsperado, BigDecimal montoPasarela) {
        return montoEsperado.compareTo(montoPasarela) == 0;
    }
}
