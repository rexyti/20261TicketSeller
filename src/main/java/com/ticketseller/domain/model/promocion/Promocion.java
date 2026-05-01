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

    public boolean estaActiva() {
        return EstadoPromocion.ACTIVA.equals(estado);
    }

    public boolean estaVigenteEn(LocalDateTime momento) {
        return momento.isAfter(fechaInicio) && momento.isBefore(fechaFin);
    }

    public void validar() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la promoción no puede ser nulo ni vacío");
        }
    }
}
