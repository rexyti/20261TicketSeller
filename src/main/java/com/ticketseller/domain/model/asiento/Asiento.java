package com.ticketseller.domain.model.asiento;

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
                .numero(fila + columna)
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(zonaId, "zonaId");
        validarObligatorio(fila, "fila");
        validarObligatorio(columna, "columna");
        validarObligatorio(numero, "numero");
        validarObligatorio(estado, "estado");
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}
