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
        adapter = new RecintoRepositoryAdapter(repository, mapper);

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
}
