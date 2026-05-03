package com.ticketseller.domain.model.bloqueos;

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
    private String destinatario;
    private CategoriaCortesia categoria;
    private String codigoUnico;
    private UUID ticketId;
    private EstadoCortesia estado;

    public void validar() {
        if (sinDestinatario()) {
            throw new IllegalArgumentException("El destinatario de la cortesía no puede estar vacío");
        }
        if (sinCodigoCortesia()) {
            throw new IllegalArgumentException("El código único de la cortesía no puede estar vacío");
        }
    }

    private boolean sinDestinatario(){
        return destinatario == null || destinatario.isBlank();
    }

    private boolean sinCodigoCortesia(){
        return codigoUnico == null || codigoUnico.isBlank();
    }
}
