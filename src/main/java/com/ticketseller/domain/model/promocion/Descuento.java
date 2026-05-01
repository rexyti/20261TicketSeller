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
}
