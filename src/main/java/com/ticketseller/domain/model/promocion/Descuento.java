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

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(promocionId, "promocionId");
        validarObligatorio(tipo, "tipo");
        validarObligatorio(valor, "valor");
        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("valor debe ser mayor a 0");
        }
        if (TipoDescuento.PORCENTAJE.equals(tipo) && valor.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Un descuento porcentual no puede ser mayor a 100");
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }
}

