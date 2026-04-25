package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCambioEstado {
    private UUID id;
    private UUID asientoId;
    private UUID eventoId;
    private String usuarioId;
    private EstadoAsiento estadoAnterior;
    private EstadoAsiento estadoNuevo;
    private Instant fechaHora;
    private String motivo;
}
