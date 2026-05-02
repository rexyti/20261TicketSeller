package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper.BloqueoPersistenceMapper;
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
class BloqueoRepositoryAdapterTest {

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
    private BloqueoR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private BloqueoRepositoryAdapter adapter;
    private UUID eventoId;
    private UUID recintoId;
    private UUID zonaId;
    private UUID asientoId;

    @BeforeEach
    void setUp() throws Exception {
        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then().block();

        recintoId = UUID.randomUUID();
        zonaId = UUID.randomUUID();
        eventoId = UUID.randomUUID();
        asientoId = UUID.randomUUID();

        databaseClient.sql("INSERT INTO recintos (id,nombre,ciudad,direccion,capacidad_maxima,telefono,fecha_creacion,compuertas_ingreso,activo,categoria) VALUES (:id,:n,:c,:d,:cap,:t,now(),:comp,true,null)")
                .bind("id", recintoId).bind("n", "Arena").bind("c", "Bogota")
                .bind("d", "Calle 1").bind("cap", 1000).bind("t", "300").bind("comp", 2)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO zonas (id,recinto_id,nombre,capacidad) VALUES (:id,:rid,:n,:cap)")
                .bind("id", zonaId).bind("rid", recintoId).bind("n", "VIP").bind("cap", 100)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO eventos (id,nombre,fecha_inicio,fecha_fin,tipo,recinto_id,estado) VALUES (:id,:n,now(),now(),:tipo,:rid,:est)")
                .bind("id", eventoId).bind("n", "Concierto").bind("tipo", "MUSICAL")
                .bind("rid", recintoId).bind("est", "ACTIVO")
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO asientos (id,fila,columna,numero,zona_id,estado,version) VALUES (:id,:f,:col,:num,:zid,:est,:ver)")
                .bind("id", asientoId).bind("f", "A").bind("col", 1).bind("num", "A1")
                .bind("zid", zonaId).bind("est", "BLOQUEADO").bind("ver", 0)
                .fetch().rowsUpdated().block();

        adapter = new BloqueoRepositoryAdapter(repository, Mappers.getMapper(BloqueoPersistenceMapper.class));
    }

    @Test
    void guardarBloqueoLoPersiste() {
        Bloqueo bloqueo = buildBloqueo(asientoId, eventoId);

        StepVerifier.create(adapter.guardar(bloqueo))
                .expectNextMatches(b -> EstadoBloqueo.ACTIVO.equals(b.getEstado())
                        && "Patrocinador Test".equals(b.getDestinatario())
                        && asientoId.equals(b.getAsientoId()))
                .verifyComplete();
    }

    @Test
    void editarBloqueoActualizaDestinatarioYAsientoSigueBloqueado() {
        Bloqueo bloqueo = buildBloqueo(asientoId, eventoId);
        adapter.guardar(bloqueo).block();

        Bloqueo actualizado = bloqueo.toBuilder().destinatario("Nuevo Sponsor").build();
        adapter.guardar(actualizado).block();

        StepVerifier.create(adapter.buscarPorId(bloqueo.getId()))
                .expectNextMatches(b -> "Nuevo Sponsor".equals(b.getDestinatario())
                        && EstadoBloqueo.ACTIVO.equals(b.getEstado()))
                .verifyComplete();

        StepVerifier.create(databaseClient.sql("SELECT estado FROM asientos WHERE id = :id")
                        .bind("id", asientoId)
                        .fetch().one())
                .expectNextMatches(row -> "BLOQUEADO".equals(row.get("estado")))
                .verifyComplete();
    }

    private Bloqueo buildBloqueo(UUID asientoIdRef, UUID eventoIdRef) {
        return Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoIdRef)
                .eventoId(eventoIdRef)
                .destinatario("Patrocinador Test")
                .fechaCreacion(LocalDateTime.now())
                .estado(EstadoBloqueo.ACTIVO)
                .build();
    }
}
