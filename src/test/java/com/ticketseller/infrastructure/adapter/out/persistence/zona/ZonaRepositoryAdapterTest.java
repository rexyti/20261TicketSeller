package com.ticketseller.infrastructure.adapter.out.persistence.zona;

import com.ticketseller.domain.model.Zona;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper.ZonaPersistenceMapper;
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
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class ZonaRepositoryAdapterTest {

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
    private ZonaR2dbcRepository repository;
    @Autowired
    private DatabaseClient databaseClient;

    private ZonaRepositoryAdapter adapter;
    private UUID recintoId;

    @BeforeEach
    void setup() throws Exception {
        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then().block();

        recintoId = UUID.randomUUID();
        databaseClient.sql("INSERT INTO recintos (id,nombre,ciudad,direccion,capacidad_maxima,telefono,fecha_creacion,compuertas_ingreso,activo,categoria) VALUES (:id,:n,:c,:d,:cap,:t,now(),:comp,true,null)")
                .bind("id", recintoId)
                .bind("n", "Arena")
                .bind("c", "Bogota")
                .bind("d", "Calle 1")
                .bind("cap", 1000)
                .bind("t", "300")
                .bind("comp", 2)
                .fetch().rowsUpdated().block();

        adapter = new ZonaRepositoryAdapter(repository, Mappers.getMapper(ZonaPersistenceMapper.class));
    }

    @Test
    void deberiaPersistirZona() {
        Zona zona = Zona.builder().id(UUID.randomUUID()).recintoId(recintoId).nombre("VIP").capacidad(200).build();

        StepVerifier.create(adapter.guardar(zona))
                .expectNextMatches(z -> "VIP".equals(z.getNombre()))
                .verifyComplete();
    }
}

