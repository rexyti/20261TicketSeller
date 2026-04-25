package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Reembolso {
    private UUID id;
    private UUID ticketId;
    private UUID ventaId;
    private BigDecimal monto;
    private TipoReembolso tipo;
    private EstadoReembolso estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaCompletado;
    private UUID agenteId;

    public void validarDatos() {
        if (id == null) throw new IllegalArgumentException("id es obligatorio");
        if (ventaId == null) throw new IllegalArgumentException("ventaId es obligatorio");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("monto debe ser mayor o igual a 0");
        }
        if (tipo == null) throw new IllegalArgumentException("tipo es obligatorio");
        if (estado == null) throw new IllegalArgumentException("estado es obligatorio");
        if (fechaSolicitud == null) throw new IllegalArgumentException("fechaSolicitud es obligatorio");
    }
}
