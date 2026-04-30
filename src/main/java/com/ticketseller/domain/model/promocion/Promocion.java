package com.ticketseller.domain.model.promocion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {
    private UUID id;
    private String nombre;
    private TipoPromocion tipo;
    private UUID eventoId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoPromocion estado;
    private TipoUsuario tipoUsuarioRestringido;

    public Promocion normalizarDatosRegistro() {
        return toBuilder()
                .nombre(nombre == null ? null : nombre.trim())
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarTextoObligatorio(nombre, "nombre");
        validarObligatorio(tipo, "tipo");
        validarObligatorio(eventoId, "eventoId");
        validarObligatorio(fechaInicio, "fechaInicio");
        validarObligatorio(fechaFin, "fechaFin");
        validarObligatorio(estado, "estado");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio");
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }
}

