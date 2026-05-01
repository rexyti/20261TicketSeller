package com.ticketseller.domain.model.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessDetails {
    private CategoriaTicket categoria;
    private String zona;
    private String compuerta;
    private LocalDateTime fechaEvento;

    public void validar() {
        if (sinCategoria()) {
            throw new IllegalArgumentException("El campo categoria es obligatorio");
        }
        if (sinZona()) {
            throw new IllegalArgumentException("El campo zona es obligatorio");
        }
        if (sinCompuerta()) {
            throw new IllegalArgumentException("El campo compuerta es obligatorio");
        }
        if (sinFechaEvento()) {
            throw new IllegalArgumentException("El campo fechaEvento es obligatorio");
        }
    }

    private boolean sinCategoria(){
        return categoria == null;
    }

    private boolean sinZona(){
        return zona == null || zona.isBlank();
    }

    private boolean sinCompuerta(){
        return compuerta == null || compuerta.isBlank();
    }

    private boolean sinFechaEvento() {
        return fechaEvento == null;
    }
}
