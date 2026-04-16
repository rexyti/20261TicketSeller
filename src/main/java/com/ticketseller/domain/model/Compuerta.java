package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Compuerta {
    private UUID id;
    private UUID recintoId;
    private UUID zonaId;
    private String nombre;
    private boolean esGeneral;
}

