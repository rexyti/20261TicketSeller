package com.ticketseller.domain.model.evento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotLiquidacion {
    private UUID eventoId;
    private Map<String, CondicionLiquidacion> condiciones;
    private LocalDateTime timestampGeneracion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CondicionLiquidacion {
        private String condicion;
        private long cantidad;
        private BigDecimal valorTotal;
    }
}
