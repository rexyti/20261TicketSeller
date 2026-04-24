package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionFinanciera {
    private UUID id;
    private UUID ventaId;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private EstadoPago estadoPago;
    private String codigoAutorizacion;
    private String respuestaPasarela;
    private LocalDateTime fecha;
    private String ip;

    public TransaccionFinanciera normalizarDatosRegistro() {
        return toBuilder()
                .codigoAutorizacion(trimOrNull(codigoAutorizacion))
                .respuestaPasarela(trimOrNull(respuestaPasarela))
                .ip(trimOrNull(ip))
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(ventaId, "ventaId");
        validarObligatorio(monto, "monto");
        validarObligatorio(metodoPago, "metodoPago");
        validarObligatorio(estadoPago, "estadoPago");
        validarObligatorio(fecha, "fecha");
        if (isMontoInvalid()) {
            throw new IllegalArgumentException("monto debe ser mayor a 0");
        }
        if (pagoAprobadoSinCodigoAutorizacion()) {
            throw new IllegalArgumentException("codigoAutorizacion es obligatorio para pagos aprobados");
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }

    private boolean isMontoInvalid(){
        return monto.compareTo(BigDecimal.ZERO) <= 0;
    }

    private boolean pagoAprobadoSinCodigoAutorizacion(){
        return estadoPago == EstadoPago.APROBADO && (codigoAutorizacion == null || codigoAutorizacion.isBlank());
    }
}

