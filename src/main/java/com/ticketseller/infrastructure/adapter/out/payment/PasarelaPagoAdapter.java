package com.ticketseller.infrastructure.adapter.out.payment;

import com.ticketseller.domain.port.out.PasarelaPagoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

// NEEDS CLARIFICATION: pasarela no definida - usando mock
@Component
@Slf4j
public class PasarelaPagoAdapter implements PasarelaPagoPort {

    @Override
    public Mono<RespuestaPago> procesarPago(UUID ventaId, BigDecimal monto, String metodoPago) {
        log.info("Procesando pago para venta {}, monto {}, metodo {}", ventaId, monto, metodoPago);
        
        // Mock: simular aprobación con 90% probabilidad
        boolean aprobada = Math.random() > 0.1;
        
        if (aprobada) {
            return Mono.just(new RespuestaPago(
                    true,
                    "AUTH-" + UUID.randomUUID().toString().substring(0, 8),
                    "Pago aprobado exitosamente",
                    "{\"status\":\"approved\",\"code\":\"00\"}"
            ));
        } else {
            return Mono.just(new RespuestaPago(
                    false,
                    null,
                    "Fondos insuficientes",
                    "{\"status\":\"declined\",\"code\":\"51\"}"
            ));
        }
    }
}
