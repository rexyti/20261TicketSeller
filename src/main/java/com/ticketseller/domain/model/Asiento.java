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
public class Asiento {
    private UUID id;
    private int fila;
    private int columna;
    private String numero;
    private UUID zonaId;
    private EstadoAsiento estado;

    public Asiento normalizarDatosRegistro() {
        return this.toBuilder()
                .numero(trimOrNull(numero))
                .build();
    }

    public void validarDatosRegistro() {
        if (fila < 0) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("La fila debe ser mayor o igual a cero");
        }
        if (columna < 0) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("La columna debe ser mayor o igual a cero");
        }
        if (numero == null || numero.isBlank()) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("El número del asiento es obligatorio");
        }
        if (zonaId == null) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("La zona del asiento es obligatoria");
        }
        if (estado == null) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("El estado del asiento es obligatorio");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}
