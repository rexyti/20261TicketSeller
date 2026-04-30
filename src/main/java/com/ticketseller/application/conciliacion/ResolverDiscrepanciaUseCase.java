package com.ticketseller.application.conciliacion;

import com.ticketseller.domain.exception.conciliacion.PagoNoEnDiscrepanciaException;
import com.ticketseller.domain.exception.conciliacion.TransaccionNoConfirmadaException;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.repository.PagoRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ResolverDiscrepanciaUseCase {

    private final PagoRepositoryPort pagoRepositoryPort;
    private final VentaRepositoryPort ventaRepositoryPort;

    public Mono<Pago> ejecutar(UUID pagoId, boolean confirmar, UUID agenteId, String justificacion) {
        if (noHayJustificacion(justificacion)) {
            return Mono.error(new IllegalArgumentException("justificacion es obligatoria"));
        }
        if (agenteIdInvalido(agenteId)) {
            return Mono.error(new IllegalArgumentException("agenteId es obligatorio"));
        }

        return pagoRepositoryPort.buscarPorId(pagoId)
                .switchIfEmpty(Mono.error(new TransaccionNoConfirmadaException(pagoId)))
                .flatMap(pago -> resolverDiscrepancia(pago, confirmar, agenteId, justificacion));
    }

    private boolean noHayJustificacion(String justificacion) {
        return justificacion == null || justificacion.isBlank();
    }

    private boolean agenteIdInvalido(UUID agenteId) {
        return agenteId == null;
    }

    private Mono<Pago> resolverDiscrepancia(Pago pago, boolean confirmar, UUID agenteId, String justificacion) {
        if (pagoNoEnDiscrepancia(pago)) {
            return Mono.error(new PagoNoEnDiscrepanciaException(
                    "El pago no está en estado EN_DISCREPANCIA: " + pago.getEstado()));
        }
        return confirmar
                ? confirmarManualmente(pago, agenteId, justificacion.trim())
                : rechazarDiscrepancia(pago, agenteId, justificacion.trim());
    }

    private boolean pagoNoEnDiscrepancia(Pago pago) {
        return !EstadoConciliacion.EN_DISCREPANCIA.equals(pago.getEstado());
    }

    private Mono<Pago> confirmarManualmente(Pago pago, UUID agenteId, String justificacion) {
        return ventaRepositoryPort.actualizarEstado(pago.getVentaId(), EstadoVenta.COMPLETADA)
                .then(pagoRepositoryPort.actualizarConResolucion(
                        pago.getId(), EstadoConciliacion.CONFIRMADO_MANUALMENTE, agenteId, justificacion));
    }

    private Mono<Pago> rechazarDiscrepancia(Pago pago, UUID agenteId, String justificacion) {
        return ventaRepositoryPort.actualizarEstado(pago.getVentaId(), EstadoVenta.FALLIDA)
                .then(pagoRepositoryPort.actualizarConResolucion(
                        pago.getId(), EstadoConciliacion.EXPIRADO, agenteId, justificacion));
    }
}
