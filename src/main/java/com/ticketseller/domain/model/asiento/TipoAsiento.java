package com.ticketseller.domain.model.asiento;

import com.ticketseller.domain.exception.asiento.NombreTipoAsientoVacioException;
import com.ticketseller.domain.exception.asiento.TipoAsientoInvalidoException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TipoAsiento {
    private UUID id;
    private String nombre;
    private String descripcion;
    private EstadoTipoAsiento estado;

    public TipoAsiento normalizarDatosRegistro() {
        return this.toBuilder()
                .nombre(trimOrNull(nombre))
                .descripcion(trimOrNull(descripcion))
                .build();
    }

    public void validarDatosRegistro() {
        if (nombre == null || nombre.isBlank()) {
            throw new NombreTipoAsientoVacioException("El campo nombre es obligatorio");
        }
        if (estado == null) {
            throw new TipoAsientoInvalidoException("El estado del tipo de asiento es obligatorio");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}
