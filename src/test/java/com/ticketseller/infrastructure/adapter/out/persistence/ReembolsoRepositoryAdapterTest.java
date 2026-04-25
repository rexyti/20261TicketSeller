package com.ticketseller.infrastructure.adapter.out.persistence;

import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.domain.model.TipoReembolso;
import com.ticketseller.infrastructure.adapter.out.persistence.mapper.ReembolsoPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class ReembolsoRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketseller")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432)
                + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private ReembolsoR2dbcRepository repository;
    @Autowired
    private DatabaseClient databaseClient;

    private ReembolsoRepositoryAdapter adapter;
    private UUID ticketId;
    private UUID ventaId;

    @BeforeEach
    void setup() throws Exception {
        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then().block();

        UUID recintoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        ventaId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        // Insert required data for foreign keys
        databaseClient.sql("INSERT INTO recintos (id,nombre,ciudad,direccion,capacidad_maxima,telefono,fecha_creacion,compuertas_ingreso) VALUES (?,?,?,?,?,?,now(),?)")
                .bind(0, recintoId).bind(1, "R1").bind(2, "C1").bind(3, "D1").bind(4, 100).bind(5, "1").bind(6, 1).fetch().rowsUpdated().block();
        databaseClient.sql("INSERT INTO zonas (id,recinto_id,nombre,capacidad) VALUES (?,?,?,?)")
                .bind(0, zonaId).bind(1, recintoId).bind(2, "Z1").bind(3, 10).fetch().rowsUpdated().block();
        databaseClient.sql("INSERT INTO eventos (id,nombre,fecha_inicio,fecha_fin,tipo,recinto_id,estado) VALUES (?,?,?,?,?,?,?)")
                .bind(0, eventoId).bind(1, "E1").bind(2, LocalDateTime.now()).bind(3, LocalDateTime.now().plusHours(1)).bind(4, "T1").bind(5, recintoId).bind(6, "ACTIVO").fetch().rowsUpdated().block();
        databaseClient.sql("INSERT INTO ventas (id,comprador_id,evento_id,estado,fecha_creacion,fecha_expiracion,total) VALUES (?,?,?,?,now(),now(),?)")
                .bind(0, ventaId).bind(1, UUID.randomUUID()).bind(2, eventoId).bind(3, "COMPLETA").bind(4, 100).fetch().rowsUpdated().block();
        databaseClient.sql("INSERT INTO tickets (id,venta_id,evento_id,zona_id,estado,precio) VALUES (?,?,?,?,?,?)")
                .bind(0, ticketId).bind(1, ventaId).bind(2, eventoId).bind(3, zonaId).bind(4, "VENDIDO").bind(5, 100).fetch().rowsUpdated().block();

        adapter = new ReembolsoRepositoryAdapter(repository, Mappers.getMapper(ReembolsoPersistenceMapper.class));
    }

    @Test
    void deberiaPersistirReembolso() {
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.valueOf(100))
                .estado(EstadoReembolso.PENDIENTE)
                .tipo(TipoReembolso.TOTAL)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        StepVerifier.create(adapter.save(reembolso))
                .expectNextMatches(r -> r.getTicketId().equals(ticketId) && r.getEstado() == EstadoReembolso.PENDIENTE)
                .verifyComplete();
    }
}
