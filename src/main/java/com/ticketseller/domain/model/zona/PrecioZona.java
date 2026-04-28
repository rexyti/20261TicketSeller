package com.ticketseller.domain.model.zona;

import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
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
public class PrecioZona {
    private UUID id;
    private UUID eventoId;
    private UUID zonaId;
    private BigDecimal precio;

    public void validarDatosRegistro() {
        if (zonaNoSeleccionada()) {
            throw new ZonaSinPrecioException("La zona es obligatoria");
        }
        if (precioInvalido()) {
            throw new ZonaSinPrecioException("El precio de la zona debe ser mayor a cero");
        }
    }

    private boolean zonaNoSeleccionada(){
        return zonaId == null;
    }

    private boolean precioInvalido(){
        return precio == null || precio.compareTo(BigDecimal.ZERO) <= 0;
    }
}

