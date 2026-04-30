package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.VentaPersistenceMapper;
import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class VentaRepositoryAdapter implements VentaRepositoryPort {

    private final VentaR2dbcRepository repository;
    private final VentaPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Venta> guardar(Venta venta) {
        return repository.save(mapper.toEntity(venta)).map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarVentasExpiradas(LocalDateTime fechaCorte) {
        return repository.findByEstadoAndFechaExpiracionBefore(EstadoVenta.RESERVADA.name(), fechaCorte)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarPorCompradorId(UUID compradorId) {
        return repository.findByCompradorId(compradorId).map(mapper::toDomain);
    }

    @Override
    public Mono<Venta> actualizarEstado(UUID id, EstadoVenta estado) {
        return repository.findById(id)
                .map(entity -> entity.toBuilder().estado(estado.name()).build())
                .flatMap(repository::save)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Venta> buscarConFiltros(EstadoVenta estado, LocalDateTime fechaInicio, LocalDateTime fechaFin, UUID eventoId) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, comprador_id, evento_id, estado, fecha_creacion, fecha_expiracion, total FROM ventas WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (estado != null) {
            sql.append(" AND estado = :estado");
            params.put("estado", estado.name());
        }
        if (fechaInicio != null) {
            sql.append(" AND fecha_creacion >= :fechaInicio");
            params.put("fechaInicio", fechaInicio);
        }
        if (fechaFin != null) {
            sql.append(" AND fecha_creacion <= :fechaFin");
            params.put("fechaFin", fechaFin);
        }
        if (eventoId != null) {
            sql.append(" AND evento_id = :eventoId");
            params.put("eventoId", eventoId);
        }
        sql.append(" ORDER BY fecha_creacion DESC");

        return bindAll(databaseClient.sql(sql.toString()), params)
                .map((row, meta) -> mapper.toDomain(rowToEntity(row)))
                .all();
    }

    @Override
    public Mono<Venta> actualizarEstadoCondicional(UUID id, EstadoVenta estadoActual, EstadoVenta nuevoEstado) {
        return databaseClient.sql("UPDATE ventas SET estado = :nuevo WHERE id = :id AND estado = :actual")
                .bind("nuevo", nuevoEstado.name())
                .bind("id", id)
                .bind("actual", estadoActual.name())
                .fetch()
                .rowsUpdated()
                .flatMap(rows -> rows > 0
                        ? repository.findById(id).map(mapper::toDomain)
                        : Mono.empty());
    }

    private DatabaseClient.GenericExecuteSpec bindAll(DatabaseClient.GenericExecuteSpec spec, Map<String, Object> params) {
        DatabaseClient.GenericExecuteSpec current = spec;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            current = current.bind(entry.getKey(), entry.getValue());
        }
        return current;
    }

    private VentaEntity rowToEntity(Row row) {
        return VentaEntity.builder()
                .id(row.get("id", UUID.class))
                .compradorId(row.get("comprador_id", UUID.class))
                .eventoId(row.get("evento_id", UUID.class))
                .estado(row.get("estado", String.class))
                .fechaCreacion(row.get("fecha_creacion", LocalDateTime.class))
                .fechaExpiracion(row.get("fecha_expiracion", LocalDateTime.class))
                .total(row.get("total", BigDecimal.class))
                .build();
    }
}
