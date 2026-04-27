package com.ticketseller.infrastructure.adapter.out.persistence.bloqueo;

import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueo.mapper.BloqueoPersistenceMapper;
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
import java.util.List;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class BloqueoRepositoryAdapterTest {

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
    private BloqueoR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private BloqueoRepositoryAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        BloqueoPersistenceMapper mapper = Mappers.getMapper(BloqueoPersistenceMapper.class);
        adapter = new BloqueoRepositoryAdapter(repository, mapper);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();
    }

    @Test
    void deberiaGuardarYConsultarBloqueosPorEvento() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, asientoId);

        Bloqueo bloqueo = Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario("Patrocinador A")
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(2))
                .estado(EstadoBloqueo.ACTIVO)
                .build();

        StepVerifier.create(adapter.guardar(bloqueo))
                .expectNextMatches(saved -> saved.getId().equals(bloqueo.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorEventoId(eventoId).collectList())
                .expectNextMatches(list -> list.size() == 1
                        && "Patrocinador A".equals(list.getFirst().getDestinatario()))
                .verifyComplete();
    }

    @Test
    void deberiaFiltrarBloqueosActivosYBuscarActivoPorAsiento() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID asientoActivo = UUID.randomUUID();
        UUID asientoLiberado = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, asientoActivo);
        insertarAsiento(zonaId, asientoLiberado);

        Bloqueo activo = Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoActivo)
                .eventoId(eventoId)
                .destinatario("Activo")
                .fechaCreacion(LocalDateTime.now())
                .estado(EstadoBloqueo.ACTIVO)
                .build();
        Bloqueo liberado = Bloqueo.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoLiberado)
                .eventoId(eventoId)
                .destinatario("Liberado")
                .fechaCreacion(LocalDateTime.now())
                .estado(EstadoBloqueo.LIBERADO)
                .build();

        StepVerifier.create(adapter.guardarTodos(List.of(activo, liberado)).collectList())
                .expectNextMatches(list -> list.size() == 2)
                .verifyComplete();

        StepVerifier.create(adapter.buscarActivosPorEventoId(eventoId).collectList())
                .expectNextMatches(list -> list.size() == 1
                        && EstadoBloqueo.ACTIVO.equals(list.getFirst().getEstado()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarActivoPorAsientoId(asientoActivo))
                .expectNextMatches(found -> found.getAsientoId().equals(asientoActivo))
                .verifyComplete();
    }

    private void insertarDatosBase(UUID recintoId, UUID zonaId, UUID eventoId, UUID asientoId) {
        databaseClient.sql("""
                        INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion,
                        compuertas_ingreso, activo)
                        VALUES ($1, 'Arena', 'Bogota', 'Calle 1', 5000, '300123', now(), 2, true)
                        """)
                .bind(0, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO zonas(id, recinto_id, nombre, capacidad) VALUES ($1, $2, 'Platea', 100)")
                .bind(0, zonaId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO eventos(id, nombre, fecha_inicio, fecha_fin, tipo, recinto_id, estado)
                        VALUES ($1, 'Evento', now(), now() + interval '2 hours', 'MUSICAL', $2, 'ACTIVO')
                        """)
                .bind(0, eventoId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        insertarAsiento(zonaId, asientoId);
    }

    private void insertarAsiento(UUID zonaId, UUID asientoId) {
        databaseClient.sql("""
                        INSERT INTO asientos(id, fila, columna, numero, zona_id, estado, existente, version, expira_en)
                        VALUES ($1, 1, 1, 'A1', $2, 'DISPONIBLE', true, 0, null)
                        """)
                .bind(0, asientoId)
                .bind(1, zonaId)
                .fetch().rowsUpdated().block();
    }
}
