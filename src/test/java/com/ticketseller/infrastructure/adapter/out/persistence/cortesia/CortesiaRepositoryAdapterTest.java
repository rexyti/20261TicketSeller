package com.ticketseller.infrastructure.adapter.out.persistence.cortesia;

import com.ticketseller.domain.model.CategoriaCortesia;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.model.EstadoCortesia;
import com.ticketseller.infrastructure.adapter.out.persistence.cortesia.mapper.CortesiaPersistenceMapper;
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
class CortesiaRepositoryAdapterTest {

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
    private CortesiaR2dbcRepository repository;

    @Autowired
    private DatabaseClient databaseClient;

    private CortesiaRepositoryAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        CortesiaPersistenceMapper mapper = Mappers.getMapper(CortesiaPersistenceMapper.class);
        adapter = new CortesiaRepositoryAdapter(repository, mapper);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();
    }

    @Test
    void deberiaGuardarYConsultarCortesiasPorEvento() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, ticketId);

        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .asientoId(null)
                .eventoId(eventoId)
                .destinatario("Invitado Especial")
                .categoria(CategoriaCortesia.ARTISTA)
                .codigoUnico("CORT1234")
                .ticketId(ticketId)
                .estado(EstadoCortesia.GENERADA)
                .build();

        StepVerifier.create(adapter.guardar(cortesia))
                .expectNextMatches(saved -> saved.getId().equals(cortesia.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorEventoId(eventoId).collectList())
                .expectNextMatches(list -> list.size() == 1
                        && "Invitado Especial".equals(list.getFirst().getDestinatario()))
                .verifyComplete();
    }

    @Test
    void deberiaBuscarCortesiaPorId() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID cortesiaId = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, ticketId);

        Cortesia cortesia = Cortesia.builder()
                .id(cortesiaId)
                .asientoId(null)
                .eventoId(eventoId)
                .destinatario("Prensa")
                .categoria(CategoriaCortesia.PRENSA)
                .codigoUnico("PRN12345")
                .ticketId(ticketId)
                .estado(EstadoCortesia.GENERADA)
                .build();

        StepVerifier.create(adapter.guardar(cortesia))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorId(cortesiaId))
                .expectNextMatches(found -> found.getId().equals(cortesiaId)
                        && CategoriaCortesia.PRENSA.equals(found.getCategoria()))
                .verifyComplete();
    }

    private void insertarDatosBase(UUID recintoId, UUID zonaId, UUID eventoId, UUID ticketId) {
        UUID ventaId = UUID.randomUUID();

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

        databaseClient.sql("""
                        INSERT INTO ventas(id, comprador_id, evento_id, estado, fecha_creacion, fecha_expiracion, total)
                        VALUES ($1, $2, $3, 'RESERVADA', $4, $5, 0)
                        """)
                .bind(0, ventaId)
                .bind(1, UUID.randomUUID())
                .bind(2, eventoId)
                .bind(3, LocalDateTime.now())
                .bind(4, LocalDateTime.now().plusMinutes(15))
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO tickets(id, venta_id, evento_id, zona_id, codigo_qr, estado, precio, es_cortesia)
                        VALUES ($1, $2, $3, $4, 'QR', 'CORTESIA', 0, true)
                        """)
                .bind(0, ticketId)
                .bind(1, ventaId)
                .bind(2, eventoId)
                .bind(3, zonaId)
                .fetch().rowsUpdated().block();
    }
}
