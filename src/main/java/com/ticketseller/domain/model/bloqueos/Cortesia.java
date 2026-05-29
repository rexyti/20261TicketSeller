package com.ticketseller.domain.model.bloqueos;

import com.ticketseller.domain.exception.bloqueos.CortesiaYaUsadaException;
import com.ticketseller.domain.exception.bloqueos.DestinatarioCortesiaInavlidoException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Cortesia {
    private UUID id;
    private UUID asientoId;
    private UUID eventoId;
    private UUID destinatarioId;
    private String emailDestinatario;
    private CategoriaCortesia categoria;
    private UUID ticketId;
    private EstadoCortesia estado;

    public void validar() {
        if (destinatarioInvalido()) {
            throw new DestinatarioCortesiaInavlidoException("La cortesía debe tener destinatarioId o emailDestinatario");
        }
    }

    private boolean destinatarioInvalido(){
        return destinatarioId == null && (emailDestinatario == null || emailDestinatario.isBlank());
    }

    public boolean isGenerada() {
        return EstadoCortesia.GENERADA.equals(estado);
    }

    public boolean isUsada() {
        return EstadoCortesia.USADA.equals(estado);
    }

    public boolean isNoUsada() {
        return EstadoCortesia.NO_USADA.equals(estado);
    }

    public void usar() {
        if (isGenerada()) {
            this.estado = EstadoCortesia.USADA;
        }
    }

    public void marcarNoUsada() {
        if (isGenerada()) {
            this.estado = EstadoCortesia.NO_USADA;
        }
        throw new CortesiaYaUsadaException("Cortesia ya usada. No se puede modificar estado.");
    }
}
