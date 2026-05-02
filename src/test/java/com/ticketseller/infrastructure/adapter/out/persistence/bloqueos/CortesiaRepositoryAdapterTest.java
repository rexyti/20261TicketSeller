package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper.CortesiaPersistenceMapper;
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
class CortesiaRepositoryAdapterTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketseller")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerR2dbcProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost()
                + ":" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private CortesiaR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private CortesiaRepositoryAdapter adapter;
    private UUID eventoId;
    private UUID recintoId;

    @BeforeEach
    void setUp() throws Exception {
        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then().block();

        recintoId = UUID.randomUUID();
        eventoId = UUID.randomUUID();

        databaseClient.sql("INSERT INTO recintos (id,nombre,ciudad,direccion,capacidad_maxima,telefono,fecha_creacion,compuertas_ingreso,activo,categoria) VALUES (:id,:n,:c,:d,:cap,:t,now(),:comp,true,null)")
                .bind("id", recintoId).bind("n", "Arena").bind("c", "Bogota")
                .bind("d", "Calle 1").bind("cap", 1000).bind("t", "300").bind("comp", 2)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO eventos (id,nombre,fecha_inicio,fecha_fin,tipo,recinto_id,estado) VALUES (:id,:n,now(),now(),:tipo,:rid,:est)")
                .bind("id", eventoId).bind("n", "Concierto").bind("tipo", "MUSICAL")
                .bind("rid", recintoId).bind("est", "ACTIVO")
                .fetch().rowsUpdated().block();

        adapter = new CortesiaRepositoryAdapter(repository, Mappers.getMapper(CortesiaPersistenceMapper.class));
    }

    @Test
    void guardarCortesiaGeneralLaPersiste() {
        Cortesia cortesia = buildCortesiaGeneral();

        StepVerifier.create(adapter.guardar(cortesia))
                .expectNextMatches(c -> EstadoCortesia.GENERADA.equals(c.getEstado())
                        && CategoriaCortesia.PRENSA.equals(c.getCategoria())
                        && c.getCodigoUnico() != null
                        && c.getAsientoId() == null)
                .verifyComplete();
    }

    @Test
    void buscarPorEventoRetornaCortesiasDelEvento() {
        Cortesia cortesia = buildCortesiaGeneral();
        adapter.guardar(cortesia).block();

        StepVerifier.create(adapter.buscarPorEvento(eventoId))
                .expectNextMatches(c -> eventoId.equals(c.getEventoId()))
                .verifyComplete();
    }

    private Cortesia buildCortesiaGeneral() {
        return Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .destinatario("Periodista ABC")
                .categoria(CategoriaCortesia.PRENSA)
                .codigoUnico(UUID.randomUUID().toString())
                .estado(EstadoCortesia.GENERADA)
                .build();
    }
}
