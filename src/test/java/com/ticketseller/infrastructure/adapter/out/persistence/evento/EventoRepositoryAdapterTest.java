package com.ticketseller.infrastructure.adapter.out.persistence.evento;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
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
class EventoRepositoryAdapterTest {

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
    private EventoR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private EventoRepositoryAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        EventoPersistenceMapper mapper = Mappers.getMapper(EventoPersistenceMapper.class);
        adapter = new EventoRepositoryAdapter(repository, mapper);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();

        UUID recintoId = UUID.randomUUID();
        databaseClient.sql("""
                INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion, compuertas_ingreso, activo)
                VALUES($1, 'Arena', 'Bogota', 'Calle 1', 1000, '300', $2, 4, true)
                """)
                .bind(0, recintoId)
                .bind(1, LocalDateTime.now())
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Test
    void deberiaPersistirYListarEventoActivo() {
        UUID recintoId = databaseClient.sql("SELECT id FROM recintos LIMIT 1")
                .map((row, metadata) -> row.get("id", UUID.class))
                .one()
                .block();

        Evento evento = Evento.builder()
                .id(UUID.randomUUID())
                .nombre("Concierto")
                .fechaInicio(LocalDateTime.now().plusDays(2))
                .fechaFin(LocalDateTime.now().plusDays(3))
                .tipo("MUSICAL")
                .recintoId(recintoId)
                .estado(EstadoEvento.ACTIVO)
                .build();

        StepVerifier.create(adapter.guardar(evento))
                .expectNextMatches(saved -> saved.getId().equals(evento.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.listarActivos().collectList())
                .expectNextMatches(list -> list.size() == 1 && "Concierto".equals(list.getFirst().getNombre()))
                .verifyComplete();
    }
}


