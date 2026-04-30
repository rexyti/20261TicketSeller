package com.ticketseller.domain.model.transaccion;

import com.ticketseller.domain.model.venta.EstadoVenta;
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
public class HistorialEstadoVenta {
    private UUID id;
    private UUID ventaId;
    private UUID actorId;
    private EstadoVenta estadoAnterior;
    private EstadoVenta estadoNuevo;
    private String justificacion;
    private LocalDateTime fechaCambio;
}
