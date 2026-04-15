package com.ticketseller.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface PasarelaPagoPort {
    Mono<RespuestaPago> procesarPago(UUID ventaId, BigDecimal monto, String metodoPago);

    record RespuestaPago(
        boolean aprobada,
        String codigoAutorizacion,
        String mensaje,
        String respuestaCompleta
    ) {}
}
