package com.ticketseller.domain.model.evento;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotLiquidacion {
    private UUID eventoId;
    private UUID recintoId;
    private CategoriaRecinto tipoRecinto;
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
        private List<TicketInfo> tickets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private UUID ticketId;
        private BigDecimal precio;
    }
}
