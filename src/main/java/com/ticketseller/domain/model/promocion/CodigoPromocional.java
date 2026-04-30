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
public class CodigoPromocional {
    private UUID id;
    private String codigo;
    private UUID promocionId;
    private Integer usosMaximos;
    private int usosActuales;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoCodigoPromocional estado;

    public CodigoPromocional normalizarDatosRegistro() {
        return toBuilder()
                .codigo(codigo == null ? null : codigo.trim().toUpperCase())
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarTextoObligatorio(codigo, "codigo");
        validarObligatorio(promocionId, "promocionId");
        validarObligatorio(fechaInicio, "fechaInicio");
        validarObligatorio(fechaFin, "fechaFin");
        validarObligatorio(estado, "estado");
        if (usosMaximos != null && usosMaximos < 1) {
            throw new IllegalArgumentException("usosMaximos debe ser mayor a 0 cuando se define");
        }
        if (usosActuales < 0) {
            throw new IllegalArgumentException("usosActuales no puede ser negativo");
        }
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

