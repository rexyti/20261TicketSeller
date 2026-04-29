package com.ticketseller.infrastructure.adapter.out.payment;

import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public class PasarelaPagoAdapter implements PasarelaPagoPort {

    @Override
    public Mono<ResultadoPago> procesarPago(UUID ventaId, BigDecimal monto, String metodoPago) {
        // NEEDS CLARIFICATION: pasarela no definida
        if ("ERROR_PASARELA".equalsIgnoreCase(metodoPago)) {
            return Mono.error(new IllegalStateException("Error temporal procesando el pago"));
        }
        if ("FONDOS_INSUFICIENTES".equalsIgnoreCase(metodoPago)) {
            return Mono.just(new ResultadoPago(false, "RECHAZADO", null,
                    "La transaccion fue rechazada por el banco. Por favor intenta con otra tarjeta u otro medio de pago"));
        }
        return Mono.just(new ResultadoPago(true, "APROBADO", "AUTH-" + ventaId.toString().substring(0, 8),
                "Pago aprobado"));
    }

    @Override
    public Mono<ResultadoPago> procesarReembolso(UUID ventaId, BigDecimal monto, String metodoPago) {
        if ("ERROR_PASARELA".equalsIgnoreCase(metodoPago)) {
            return Mono.error(new IllegalStateException("Error temporal procesando el reembolso"));
        }
        return Mono.just(new ResultadoPago(true, "APROBADO", "REF-" + ventaId.toString().substring(0, 8),
                "Reembolso aprobado"));
    }
}

