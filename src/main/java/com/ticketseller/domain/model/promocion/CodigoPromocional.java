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

    public boolean estaVigente(LocalDateTime ahora) {
        return ahora.isAfter(fechaInicio) && ahora.isBefore(fechaFin);
    }

    public void validar() {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código no puede estar vacío");
        }
        if (usosMaximos != null && usosMaximos <= 0) {
            throw new IllegalArgumentException("Los usos máximos deben ser mayores que cero");
        }
        if (fechaInicio != null && fechaFin != null && !fechaInicio.isBefore(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }

    public boolean tieneUsosDisponibles() {
        return usosMaximos == null || usosActuales < usosMaximos;
    }
}
