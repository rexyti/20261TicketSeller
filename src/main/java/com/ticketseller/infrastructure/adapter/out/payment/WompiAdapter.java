package com.ticketseller.infrastructure.adapter.out.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter de integración con Wompi (https://docs.wompi.co).
 *
 * Métodos soportados:
 *   - TARJETA        → card          (tokenización previa requerida, ver Wompi.js)
 *   - PSE            → PSE           (requiere datos bancarios del usuario)
 *   - NEQUI          → NEQUI         (requiere número de celular)
 *   - DAVIPLATA      → DAVIPLATA     (requiere número de celular)
 *
 * Variables de entorno requeridas:
 *   WOMPI_BASE_URL       → https://sandbox.wompi.co/v1  (sandbox)
 *                          https://production.wompi.co/v1 (producción)
 *   WOMPI_PRIVATE_KEY    → prv_test_... / prv_prod_...
 */
public class WompiAdapter implements PasarelaPagoPort {

    // Wompi maneja montos en centavos (COP)
    private static final BigDecimal CENTAVOS = BigDecimal.valueOf(100);

    private static final Map<String, String> METODO_A_TIPO_WOMPI = Map.of(
            "TARJETA",   "CARD",
            "PSE",       "PSE",
            "NEQUI",     "NEQUI",
            "DAVIPLATA", "DAVIPLATA"
    );

    private final WebClient webClient;

    public WompiAdapter(String baseUrl, String privateKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + privateKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public Mono<ResultadoPago> procesarPago(UUID ventaId, BigDecimal monto, String metodoPago) {
        String tipoWompi = resolverTipoWompi(metodoPago);
        long montoCentavos = monto.multiply(CENTAVOS).longValue();

        Map<String, Object> body = Map.of(
                "amount_in_cents",    montoCentavos,
                "currency",           "COP",
                "customer_email",     "cliente@ticketseller.com", // reemplazar con email real del usuario
                "reference",          ventaId.toString(),
                "payment_method",     Map.of("type", tipoWompi)
        );

        return webClient.post()
                .uri("/transactions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WompiTransaccionResponse.class)
                .map(this::mapearResultado)
                .onErrorResume(ex -> Mono.error(
                        new IllegalStateException("Error al conectar con Wompi: " + ex.getMessage(), ex)
                ));
    }

    private String resolverTipoWompi(String metodoPago) {
        if (noHayMetodoPago(metodoPago)) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        String tipo = METODO_A_TIPO_WOMPI.get(metodoPago.toUpperCase());
        if (metodoNoSoportado(tipo)) {
            throw new IllegalArgumentException("Método de pago no soportado: " + metodoPago);
        }
        return tipo;
    }

    private boolean noHayMetodoPago(String metodoPago){
        return metodoPago == null;
    }

    private boolean metodoNoSoportado(String tipo){
        return tipo == null;
    }

    private ResultadoPago mapearResultado(WompiTransaccionResponse response) {
        if (noData(response)) {
            return new ResultadoPago(false, "ERROR", null,
                    "Respuesta inesperada de Wompi");
        }

        boolean aprobado = isPagoAprobado(response);
        String estadoPago = aprobado ? "APROBADO" : "RECHAZADO";
        String codigoAuth = response.data.id;
        String respuesta = aprobado
                ? "Pago aprobado"
                : obtenerMensajeRechazo(response.data.statusMessage);

        return new ResultadoPago(aprobado, estadoPago, codigoAuth, respuesta);
    }

    private boolean noData(WompiTransaccionResponse response){
        return response.data == null;
    }

    private boolean isPagoAprobado(WompiTransaccionResponse response){
        return "APPROVED".equalsIgnoreCase(response.data.status);
    }

    private String obtenerMensajeRechazo(String statusMessage) {
        if (noHayMensaje(statusMessage)) {
            return "La transacción fue rechazada por el banco. Por favor intenta con otro medio de pago";
        }
        return statusMessage;
    }

    private boolean noHayMensaje(String statusMessage){
        return statusMessage == null || statusMessage.isBlank();
    }

    // -------------------------------------------------------------------------
    // DTOs internos para deserializar la respuesta de Wompi
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WompiTransaccionResponse {
        @JsonProperty("data")
        public WompiTransaccionData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class WompiTransaccionData {
        @JsonProperty("id")
        public String id;

        @JsonProperty("status")
        public String status;             // APPROVED | DECLINED | VOIDED | ERROR

        @JsonProperty("status_message")
        public String statusMessage;

        @JsonProperty("reference")
        public String reference;

        @JsonProperty("amount_in_cents")
        public Long amountInCents;
    }
}