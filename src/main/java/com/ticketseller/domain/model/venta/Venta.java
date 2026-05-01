package com.ticketseller.domain.model.venta;

import com.ticketseller.domain.exception.transaccion.TransicionVentaInvalidaException;
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
public class Venta {
    private UUID id;
    private UUID compradorId;
    private UUID eventoId;
    private EstadoVenta estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private BigDecimal total;

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(compradorId, "compradorId");
        validarObligatorio(eventoId, "eventoId");
        validarObligatorio(estado, "estado");
        validarObligatorio(fechaCreacion, "fechaCreacion");
        validarObligatorio(fechaExpiracion, "fechaExpiracion");
        validarObligatorio(total, "total");
        if (fechaExpiracion.isBefore(fechaCreacion)) {
            throw new IllegalArgumentException("fechaExpiracion debe ser posterior a fechaCreacion");
        }
        if (isTotalInvalid()) {
            throw new IllegalArgumentException("total debe ser mayor o igual a 0");
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private boolean isTotalInvalid(){
        return total.compareTo(BigDecimal.ZERO) < 0;
    }

    public void validarTransicionA(EstadoVenta destino) {
        if (transicionInvalida(destino)) {
            throw new TransicionVentaInvalidaException(estado, destino);
        }
    }

    private boolean transicionInvalida(EstadoVenta destino) {
        return estado.equals(destino) || !estado.transicionesPermitidas().contains(destino);
    }
}

