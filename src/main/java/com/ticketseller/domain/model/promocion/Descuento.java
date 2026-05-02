package com.ticketseller.domain.model.promocion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Descuento {

    private UUID id;
    private UUID promocionId;
    private TipoDescuento tipo;
    private BigDecimal valor;
    private UUID zonaId;
    private boolean acumulable;

    public boolean aplicaAZona(UUID zona) {
        return zonaId == null || zonaId.equals(zona);
    }

    public void validar() {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor del descuento no puede ser negativo");
        }
        if (TipoDescuento.PORCENTAJE.equals(tipo) && valor.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de descuento no puede superar el 100%");
        }
    }
}
