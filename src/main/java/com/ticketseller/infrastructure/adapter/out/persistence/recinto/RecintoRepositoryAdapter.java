package com.ticketseller.infrastructure.adapter.out.persistence.recinto;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.shared.Pagina;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.StringUtils;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.r2dbc.spi.Row;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

public class RecintoRepositoryAdapter implements RecintoRepositoryPort {

    private final RecintoR2dbcRepository repository;
    private final RecintoPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    public RecintoRepositoryAdapter(RecintoR2dbcRepository repository,
                                    RecintoPersistenceMapper mapper,
                                    DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Recinto> guardar(Recinto recinto) {
        return repository.save(mapper.toEntity(recinto)).map(mapper::toDomain);
    }

    @Override
    public Mono<Recinto> buscarPorId(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Recinto> buscarPorNombreYCiudad(String nombre, String ciudad) {
        return repository.findByNombreIgnoreCaseAndCiudadIgnoreCase(nombre, ciudad).map(mapper::toDomain);
    }

    @Override
    public Flux<Recinto> listarTodos() {
        return repository.findAll().map(mapper::toDomain);
    }

    @Override
    public Mono<Pagina<Recinto>> listarFiltrados(String nombre,
                                                 CategoriaRecinto categoria,
                                                 String ciudad,
                                                 Boolean activo,
                                                 int page,
                                                 int size,
                                                 String sort) {
        int pageNumber = Math.max(0, page);
        int pageSize = size <= 0 ? 10 : size;
        int offset = pageNumber * pageSize;

        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (StringUtils.hasText(nombre)) {
            whereClause.append(" AND LOWER(nombre) LIKE LOWER(:nombre)");
            params.put("nombre", "%" + nombre.trim() + "%");
        }
        if (categoria != null) {
            whereClause.append(" AND categoria = :categoria");
            params.put("categoria", categoria.name());
        }
        if (StringUtils.hasText(ciudad)) {
            whereClause.append(" AND LOWER(ciudad) = LOWER(:ciudad)");
            params.put("ciudad", ciudad.trim());
        }
        if (activo != null) {
            whereClause.append(" AND activo = :activo");
            params.put("activo", activo);
        }

        SortSpec sortSpec = resolveSort(sort);

        String selectSql = "SELECT id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion, "
                + "compuertas_ingreso, activo, categoria FROM recintos"
                + whereClause
                + " ORDER BY " + sortSpec.field() + " " + sortSpec.direction()
                + " LIMIT :limit OFFSET :offset";

        String countSql = "SELECT COUNT(*) AS total FROM recintos" + whereClause;

        Mono<List<Recinto>> data = bindAll(databaseClient.sql(selectSql), params)
                .bind("limit", pageSize)
                .bind("offset", offset)
                .map((row, metadata) -> mapper.toDomain(toEntity(row)))
                .all()
                .collectList();

        Mono<Long> total = bindAll(databaseClient.sql(countSql), params)
                .map((row, metadata) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);

        return Mono.zip(data, total)
                .map(tuple -> new Pagina<>(tuple.getT1(), tuple.getT2(), pageNumber, pageSize));
    }

    @Override
    public Mono<Boolean> tieneEventosFuturos(UUID recintoId) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM eventos
                            WHERE recinto_id = $1
                              AND estado <> 'CANCELADO'
                              AND fecha_inicio > $2
                        ) AS existe
                        """)
                .bind(0, recintoId)
                .bind(1, LocalDateTime.now())
                .map((row, metadata) -> row.get("existe", Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<Recinto> buscarPorCategoria(CategoriaRecinto categoria) {
        return repository.findByCategoriaIgnoreCase(categoria.name()).map(mapper::toDomain);
    }

    @Override
    public Flux<Recinto> buscarPorCiudad(String ciudad) {
        return repository.findByCiudadIgnoreCase(ciudad).map(mapper::toDomain);
    }

    @Override
    public Mono<Boolean> tieneTicketsVendidos(UUID recintoId) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM tickets t
                            INNER JOIN eventos e ON e.id = t.evento_id
                            WHERE e.recinto_id = $1
                              AND t.estado = 'VENDIDO'
                        ) AS existe
                        """)
                .bind(0, recintoId)
                .map((row, metadata) -> row.get("existe", Boolean.class))
                .one()
                .defaultIfEmpty(false);
    }

    private GenericExecuteSpec bindAll(GenericExecuteSpec spec, Map<String, Object> params) {
        GenericExecuteSpec current = spec;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            current = current.bind(entry.getKey(), entry.getValue());
        }
        return current;
    }

    private RecintoEntity toEntity(Row row) {
        return RecintoEntity.builder()
                .id(row.get("id", UUID.class))
                .nombre(row.get("nombre", String.class))
                .ciudad(row.get("ciudad", String.class))
                .direccion(row.get("direccion", String.class))
                .capacidadMaxima(row.get("capacidad_maxima", Integer.class))
                .telefono(row.get("telefono", String.class))
                .fechaCreacion(row.get("fecha_creacion", java.time.LocalDateTime.class))
                .compuertasIngreso(row.get("compuertas_ingreso", Integer.class))
                .activo(row.get("activo", Boolean.class))
                .categoria(row.get("categoria", String.class))
                .build();
    }

    private SortSpec resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return new SortSpec("nombre", "ASC");
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim().toLowerCase();
        String dbField = switch (field) {
            case "ciudad" -> "ciudad";
            case "capacidadmaxima" -> "capacidad_maxima";
            default -> "nombre";
        };
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
        return new SortSpec(dbField, desc ? "DESC" : "ASC");
    }

    private record SortSpec(String field, String direction) {
    }
}

