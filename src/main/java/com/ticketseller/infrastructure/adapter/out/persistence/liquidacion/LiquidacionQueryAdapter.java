package com.ticketseller.infrastructure.adapter.out.persistence.liquidacion;

import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LiquidacionQueryAdapter implements LiquidacionQueryPort {

    private final DatabaseClient databaseClient;

    public LiquidacionQueryAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<SnapshotLiquidacion> obtenerSnapshotPorEvento(UUID eventoId) {
        // TODO: coordinar con Módulo 2 cómo se registra el check-in en el ticket
        String sql = """
                SELECT
                    CASE
                        WHEN t.es_cortesia = true THEN 'CORTESIA'
                        WHEN t.estado = 'ANULADO' OR t.estado = 'REEMBOLSADO' THEN 'CANCELADO'
                        WHEN t.estado = 'VENDIDO' THEN 'VENDIDO_SIN_ASISTENCIA'
                        ELSE 'OTRO'
                    END AS condicion,
                    COUNT(*) AS cantidad,
                    COALESCE(SUM(t.precio), 0) AS valor_total
                FROM tickets t
                WHERE t.evento_id = :eventoId
                  AND t.estado IN ('VENDIDO', 'ANULADO', 'REEMBOLSADO')
                GROUP BY condicion
                """;
        // NEEDS CLARIFICATION: confirmar con el equipo — tickets en estados intermedios (RESERVADO, EXPIRADO) se excluyen

        return databaseClient.sql(sql)
                .bind("eventoId", eventoId)
                .map((row, metadata) -> SnapshotLiquidacion.CondicionLiquidacion.builder()
                        .condicion(row.get("condicion", String.class))
                        .cantidad(row.get("cantidad", Long.class))
                        .valorTotal(row.get("valor_total", BigDecimal.class))
                        .build())
                .all()
                .collectMap(
                        SnapshotLiquidacion.CondicionLiquidacion::getCondicion,
                        condicion -> condicion
                )
                .map(condiciones -> SnapshotLiquidacion.builder()
                        .eventoId(eventoId)
                        .condiciones(condiciones)
                        .timestampGeneracion(LocalDateTime.now())
                        .build());
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
}
