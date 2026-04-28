package com.ticketseller.domain.model.zona;

import com.ticketseller.domain.exception.CompuertaInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Compuerta {
    private UUID id;
    private UUID recintoId;
    private UUID zonaId;
    private String nombre;
    private boolean esGeneral;

    public Compuerta normalizarDatosRegistro() {
        return this.toBuilder()
                .nombre(trimOrNull(nombre))
                .build();
    }

    public void validarDatosRegistro() {
        if (nombre == null || nombre.isBlank()) {
            throw new CompuertaInvalidaException("El campo nombre es obligatorio");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}

