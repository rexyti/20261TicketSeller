package com.ticketseller.domain.model;

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
public class Recinto {
    private UUID id;
    private String nombre;
    private String ciudad;
    private String direccion;
    private Integer capacidadMaxima;
    private String telefono;
    private LocalDateTime fechaCreacion;
    private Integer compuertasIngreso;
    private boolean activo;
    private CategoriaRecinto categoria;

    public void desactivar(){
        if (activo)
            activo = false;
    }
}

