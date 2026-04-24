package com.ticketseller.infrastructure.adapter.out.persistence.recinto;

import com.ticketseller.domain.model.Recinto;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class RecintoRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketseller")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private RecintoR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private RecintoRepositoryAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        RecintoPersistenceMapper mapper = Mappers.getMapper(RecintoPersistenceMapper.class);
        adapter = new RecintoRepositoryAdapter(repository, mapper, databaseClient);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();
    }

    @Test
    void deberiaPersistirYListarRecinto() {
        Recinto recinto = Recinto.builder()
                .id(UUID.randomUUID())
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .fechaCreacion(LocalDateTime.now())
                .compuertasIngreso(4)
                .activo(true)
                .build();

        StepVerifier.create(adapter.guardar(recinto))
                .expectNextMatches(saved -> saved.getId().equals(recinto.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.listarTodos().collectList())
                .expectNextMatches(list -> list.size() == 1 && "Movistar Arena".equals(list.getFirst().getNombre()))
                .verifyComplete();
    }

    @Test
    void deberiaDetectarTicketsVendidosPorRecinto() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        databaseClient.sql("""
                        INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion,
                        compuertas_ingreso, activo, categoria)
                        VALUES ($1, 'Arena', 'Bogota', 'Calle 1', 5000, '300123', now(), 2, true, 'MEDIANO')
                        """)
                .bind(0, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO zonas(id, recinto_id, nombre, capacidad) VALUES ($1, $2, 'Platea', 100)")
                .bind(0, zonaId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO eventos(id, nombre, fecha_inicio, fecha_fin, tipo, recinto_id, estado)
                        VALUES ($1, 'Concierto', now() + interval '1 day', now() + interval '1 day 2 hours',
                        'MUSICAL', $2, 'ACTIVO')
                        """)
                .bind(0, eventoId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO ventas(id, comprador_id, evento_id, estado, fecha_creacion, fecha_expiracion, total)
                        VALUES ($1, $2, $3, 'COMPLETADA', now(), now() + interval '15 minutes', 100)
                        """)
                .bind(0, ventaId)
                .bind(1, UUID.randomUUID())
                .bind(2, eventoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO tickets(id, venta_id, evento_id, zona_id, compuerta_id, codigo_qr, estado, precio, es_cortesia)
                        VALUES ($1, $2, $3, $4, null, null, 'VENDIDO', 100, false)
                        """)
                .bind(0, ticketId)
                .bind(1, ventaId)
                .bind(2, eventoId)
                .bind(3, zonaId)
                .fetch().rowsUpdated().block();

        StepVerifier.create(adapter.tieneTicketsVendidos(recintoId))
                .expectNext(true)
                .verifyComplete();
    }
}
