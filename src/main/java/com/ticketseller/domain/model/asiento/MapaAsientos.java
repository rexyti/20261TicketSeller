package com.ticketseller.domain.model.asiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MapaAsientos {
    private UUID recintoId;
    private int filas;
    private int columnas;
}
