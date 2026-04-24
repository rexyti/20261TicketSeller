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
    private String fila;
    private Integer columna;
    private String numero;
    private UUID zonaId;
    private TipoAsiento tipoAsiento;
    private EstadoAsiento estado;

    public Asiento normalizarDatosRegistro() {
        return this.toBuilder()
                .fila(trimOrNull(fila))
                .numero(trimOrNull(numero))
                .build();
    }

    public void validarDatosRegistro() {
        if (fila == null || fila.isBlank()) {
            throw new com.ticketseller.domain.exception.AsientoInvalidoException("La fila del asiento es obligatoria");
        }
        if (columna == null || columna < 0) {
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
