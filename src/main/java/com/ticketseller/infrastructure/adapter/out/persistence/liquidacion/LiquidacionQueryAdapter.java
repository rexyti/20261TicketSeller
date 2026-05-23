package com.ticketseller.infrastructure.adapter.out.persistence.liquidacion;

import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class LiquidacionQueryAdapter implements LiquidacionQueryPort {

    private final DatabaseClient databaseClient;

    public LiquidacionQueryAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<SnapshotLiquidacion> obtenerSnapshotPorEvento(UUID eventoId) {
        String sql = """
                SELECT
                    CASE
                        WHEN t.es_cortesia = true THEN 'CORTESIA'
                        WHEN t.estado IN ('ANULADO', 'REEMBOLSADO') THEN 'CANCELADO'
                        WHEN t.estado = 'VENDIDO' THEN 'VENDIDO'
                        ELSE 'OTRO'
                    END AS condicion,
                    t.id AS ticket_id,
                    t.precio
                FROM tickets t
                WHERE t.evento_id = :eventoId
                  AND t.estado IN ('VENDIDO', 'ANULADO', 'REEMBOLSADO')
                """;

        return databaseClient.sql(sql)
                .bind("eventoId", eventoId)
                .map((row, metadata) -> new TicketRow(
                        row.get("condicion", String.class),
                        row.get("ticket_id", UUID.class),
                        row.get("precio", BigDecimal.class)))
                .all()
                .collectList()
                .map(rows -> buildSnapshot(eventoId, rows));
    }

    private SnapshotLiquidacion buildSnapshot(UUID eventoId, List<TicketRow> rows) {
        Map<String, List<TicketRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(TicketRow::condicion, LinkedHashMap::new, Collectors.toList()));

        Map<String, SnapshotLiquidacion.CondicionLiquidacion> condiciones = new LinkedHashMap<>();
        grouped.forEach((condicion, ticketRows) -> {
            List<SnapshotLiquidacion.TicketInfo> tickets = ticketRows.stream()
                    .map(row -> SnapshotLiquidacion.TicketInfo.builder()
                            .ticketId(row.ticketId())
                            .precio(row.precio() != null ? row.precio() : BigDecimal.ZERO)
                            .build())
                    .toList();
            BigDecimal valorTotal = tickets.stream()
                    .map(SnapshotLiquidacion.TicketInfo::getPrecio)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            condiciones.put(condicion, SnapshotLiquidacion.CondicionLiquidacion.builder()
                    .condicion(condicion)
                    .cantidad(tickets.size())
                    .valorTotal(valorTotal)
                    .tickets(tickets)
                    .build());
        });

        return SnapshotLiquidacion.builder()
                .eventoId(eventoId)
                .condiciones(condiciones)
                .timestampGeneracion(LocalDateTime.now())
                .build();
    }

    @Override
    public Mono<Map<String, BigDecimal>> obtenerRecaudoPorEvento(UUID eventoId) {
        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN t.estado = 'VENDIDO' AND t.es_cortesia = false THEN t.precio ELSE 0 END), 0) AS recaudo_regular,
                    COALESCE(SUM(CASE WHEN t.estado = 'VENDIDO' AND t.es_cortesia = true THEN t.precio ELSE 0 END), 0) AS recaudo_cortesia,
                    COALESCE(SUM(CASE WHEN t.estado IN ('ANULADO', 'REEMBOLSADO') THEN t.precio ELSE 0 END), 0) AS cancelaciones,
                    COALESCE(SUM(CASE WHEN t.estado = 'VENDIDO' THEN t.precio ELSE 0 END), 0)
                        - COALESCE(SUM(CASE WHEN t.estado IN ('ANULADO', 'REEMBOLSADO') THEN t.precio ELSE 0 END), 0) AS recaudo_neto
                FROM tickets t
                WHERE t.evento_id = :eventoId
                """;

        return databaseClient.sql(sql)
                .bind("eventoId", eventoId)
                .map((row, metadata) -> {
                    Map<String, BigDecimal> result = new HashMap<>();
                    result.put("recaudoRegular", row.get("recaudo_regular", BigDecimal.class));
                    result.put("recaudoCortesia", row.get("recaudo_cortesia", BigDecimal.class));
                    result.put("cancelaciones", row.get("cancelaciones", BigDecimal.class));
                    result.put("recaudoNeto", row.get("recaudo_neto", BigDecimal.class));
                    return result;
                })
                .one()
                .defaultIfEmpty(Map.of(
                        "recaudoRegular", BigDecimal.ZERO,
                        "recaudoCortesia", BigDecimal.ZERO,
                        "cancelaciones", BigDecimal.ZERO,
                        "recaudoNeto", BigDecimal.ZERO
                ));
    }

    private record TicketRow(String condicion, UUID ticketId, BigDecimal precio) {}
}
