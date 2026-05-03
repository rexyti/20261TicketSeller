package com.ticketseller.domain.model.zona;

import com.ticketseller.domain.exception.compuerta.CompuertaInvalidaException;
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
        if (nombreNoValido()) {
            throw new CompuertaInvalidaException("El campo nombre es obligatorio");
        }
    }

    private boolean nombreNoValido() {
        return nombre == null || nombre.isBlank();
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}

