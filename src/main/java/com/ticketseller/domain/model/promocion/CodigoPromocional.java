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
        return !ahora.isBefore(fechaInicio) && !ahora.isAfter(fechaFin);
    }

    public boolean tieneUsosDisponibles() {
        return usosMaximos == null || usosActuales < usosMaximos;
    }
}
