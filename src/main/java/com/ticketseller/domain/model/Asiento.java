package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Asiento {
    private UUID id;
    private int fila;
    private int columna;
    private String numero;
    private UUID zonaId;
    private String estado;
    private boolean existente;
}
