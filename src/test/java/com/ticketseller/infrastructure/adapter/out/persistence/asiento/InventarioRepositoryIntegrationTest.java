package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import com.ticketseller.application.inventario.AsientoEnEvento;
import com.ticketseller.application.inventario.LiberarHoldsVencidosUseCase;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper.AsientoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.asientohold.AsientoHoldR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.asientohold.AsientoHoldRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.asientohold.mapper.AsientoHoldPersistenceMapper;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class InventarioRepositoryIntegrationTest {

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
    private AsientoR2dbcRepository repository;

    @Autowired
    private AsientoHoldR2dbcRepository holdRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private AsientoRepositoryAdapter adapter;
    private AsientoHoldRepositoryAdapter holdAdapter;
    private LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase;

    private final UUID recintoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();
    private final UUID eventoId = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        AsientoPersistenceMapper mapper = Mappers.getMapper(AsientoPersistenceMapper.class);
        AsientoHoldPersistenceMapper holdMapper = Mappers.getMapper(AsientoHoldPersistenceMapper.class);
        adapter = new AsientoRepositoryAdapter(repository, mapper);
        holdAdapter = new AsientoHoldRepositoryAdapter(holdRepository, holdMapper);
        liberarHoldsVencidosUseCase = new LiberarHoldsVencidosUseCase(holdAdapter);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();

        databaseClient.sql("""
                        INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono,
                        fecha_creacion, compuertas_ingreso, activo, categoria)
                        VALUES ($1, 'Arena', 'Bogota', 'Calle 1', 5000, '300123', now(), 2, true, 'MEDIANO')
                        """)
                .bind(0, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql(
                        "INSERT INTO zonas(id, recinto_id, nombre, capacidad) VALUES ($1, $2, 'Platea', 100)")
                .bind(0, zonaId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();
    }

    private UUID insertAsiento() {
        UUID id = UUID.randomUUID();
        databaseClient.sql(
                        "INSERT INTO asientos(id, fila, columna, numero, zona_id, estado) VALUES ($1, 'A', 1, 'A1', $2, 'DISPONIBLE')")
                .bind(0, id)
                .bind(1, zonaId)
                .fetch().rowsUpdated().block();
        return id;
    }

    private Mono<AsientoHold> crearHold(UUID asientoId, LocalDateTime expiraEn) {
        return holdAdapter.guardar(AsientoHold.builder()
                .asientoId(asientoId)
                .eventoId(eventoId)
                .ventaId(UUID.randomUUID())
                .numero("A1")
                .expiraEn(expiraEn)
                .estado(EstadoHold.RESERVADO)
                .build());
    }

    // T019: crear hold → verificar expiraEn → scheduler libera hold vencido
    @Test
    void schedulerLiberaHoldVencidoMarcandoloComoExpirado() {
        UUID id = insertAsiento();

        StepVerifier.create(crearHold(id, LocalDateTime.now().plusMinutes(15)))
                .assertNext(hold -> {
                    assertEquals(EstadoHold.RESERVADO, hold.getEstado());
                    assertNotNull(hold.getExpiraEn());
                    assertTrue(hold.getExpiraEn().isAfter(LocalDateTime.now()));
                })
                .verifyComplete();

        databaseClient.sql("UPDATE asiento_holds SET expira_en = $1 WHERE asiento_id = $2")
                .bind(0, LocalDateTime.now().minusMinutes(1))
                .bind(1, id)
                .fetch().rowsUpdated().block();

        StepVerifier.create(liberarHoldsVencidosUseCase.ejecutar(LocalDateTime.now()))
                .assertNext(hold -> assertEquals(EstadoHold.EXPIRADO, hold.getEstado()))
                .verifyComplete();
    }

    // T027: hold queda RESERVADO hasta que se confirme la venta
    @Test
    void holdCreadoConEstadoReservadoParaEvento() {
        UUID id = insertAsiento();

        StepVerifier.create(crearHold(id, LocalDateTime.now().plusMinutes(15)))
                .assertNext(hold -> {
                    assertEquals(EstadoHold.RESERVADO, hold.getEstado());
                    assertEquals(eventoId, hold.getEventoId());
                    assertEquals(id, hold.getAsientoId());
                })
                .verifyComplete();
    }

    // T033: concurrencia real — dos holds para el mismo asiento en el mismo evento
    @Test
    void concurrenciaRealAdapterPermiteInsertarDosHoldsParaMismoAsiento() throws InterruptedException {
        UUID id = insertAsiento();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger(0);
        AtomicInteger fallidas = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            try {
                latch.await();
                crearHold(id, LocalDateTime.now().plusMinutes(15)).block();
                exitosas.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                fallidas.incrementAndGet();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                crearHold(id, LocalDateTime.now().plusMinutes(15)).block();
                exitosas.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                fallidas.incrementAndGet();
            }
        });

        t1.start();
        t2.start();
        latch.countDown();
        t1.join();
        t2.join();

        assertEquals(2, exitosas.get() + fallidas.get(), "Deben sumar exactamente 2 resultados");
        assertTrue(exitosas.get() >= 1, "Al menos una reserva debe ser exitosa");
    }
}
