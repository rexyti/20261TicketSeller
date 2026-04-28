package com.ticketseller.domain.repository;

import com.ticketseller.domain.model.venta.ResultadoPago;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface PasarelaPagoPort {

    Mono<ResultadoPago> procesarPago(UUID ventaId, BigDecimal monto, String metodoPago);
}

