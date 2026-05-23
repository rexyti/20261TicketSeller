package com.ticketseller.domain.model.asiento;

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
public class AsientoHold {
    private UUID id;
    private UUID asientoId;
    private UUID ventaId;
    private String numero;
    private LocalDateTime expiraEn;
    private EstadoHold estado;

    public boolean isReservado(){
        return EstadoHold.RESERVADO.equals(estado);
    }

    public boolean isExpirado(){
        return EstadoHold.EXPIRADO.equals(estado);
    }

    public boolean isCompletada(){
        return EstadoHold.COMPLETADA.equals(estado);
    }

    public boolean isActiva(){
        return expiraEn.isBefore(LocalDateTime.now()) && isReservado();
    }

    public void completar(){
        if (isReservado()){
            this.estado = EstadoHold.COMPLETADA;
        }
    }

    public void expirar(){
        if (isReservado()){
            this.estado = EstadoHold.EXPIRADO;
        }
    }
}
