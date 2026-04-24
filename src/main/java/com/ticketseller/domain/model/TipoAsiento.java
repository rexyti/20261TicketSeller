package com.ticketseller.domain.model;

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
            throw new com.ticketseller.domain.exception.NombreTipoAsientoVacioException("El campo nombre es obligatorio");
        }
        if (estado == null) {
            throw new com.ticketseller.domain.exception.TipoAsientoInvalidoException("El estado del tipo de asiento es obligatorio");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}
