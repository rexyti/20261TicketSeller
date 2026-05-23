package com.ticketseller.domain.model.bloqueos;

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
public class Bloqueo {
    private UUID id;
    private UUID asientoId;
    private UUID eventoId;
    private String destinatario;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private EstadoBloqueo estado;

    public void validar() {
        if (sinDestinatario()) {
            throw new IllegalArgumentException("El destinatario del bloqueo no puede estar vacío");
        }
        if (fechasInvalidas()) {
            throw new IllegalArgumentException("La fecha de expiración debe ser posterior a la fecha de creación");
        }
    }

    private boolean sinDestinatario(){
        return destinatario == null || destinatario.isBlank();
    }

    private boolean fechasInvalidas(){
        return fechaExpiracion != null && fechaCreacion != null
                && fechaExpiracion.isBefore(fechaCreacion);
    }

    public boolean isActivo(){
        return EstadoBloqueo.ACTIVO.equals(estado);
    }

    public boolean isLiberado(){
        return EstadoBloqueo.LIBERADO.equals(estado);
    }

    public void liberar(){
        if (isActivo()){
            this.estado = EstadoBloqueo.LIBERADO;
        }
    }
}
