package com.ticketseller.domain.model.zona;

import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Zona {
    private UUID id;
    private UUID recintoId;
    private String nombre;
    private Integer capacidad;
    private UUID tipoAsientoId;

    public Zona normalizarDatosRegistro() {
        return this.toBuilder()
                .nombre(trimOrNull(nombre))
                .build();
    }

    public void validarDatosRegistro() {
        validarTextoObligatorio(nombre);
        validarPositivo(capacidad);
    }

    private void validarTextoObligatorio(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ZonaInvalidaException("El campo 'nombre' es obligatorio");
        }
    }

    private void validarPositivo(Integer valor) {
        if (valor == null || valor < 1) {
            throw new ZonaInvalidaException("El campo 'capacidad' debe ser mayor a cero");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}
