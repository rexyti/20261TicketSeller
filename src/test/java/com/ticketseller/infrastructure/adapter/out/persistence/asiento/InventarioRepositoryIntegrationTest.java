package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.LiberarHoldsVencidosUseCase;
import com.ticketseller.application.inventario.ReservarAsientoUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.domain.exception.asiento.AsientoReservadoPorOtroException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper.AsientoPersistenceMapper;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private DatabaseClient databaseClient;

    private AsientoRepositoryAdapter adapter;
    private VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;
    private ReservarAsientoUseCase reservarAsientoUseCase;
    private LiberarHoldsVencidosUseCase liberarHoldsVencidosUseCase;
    private ConfirmarOcupacionUseCase confirmarOcupacionUseCase;

    private final UUID recintoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        AsientoPersistenceMapper mapper = Mappers.getMapper(AsientoPersistenceMapper.class);
        adapter = new AsientoRepositoryAdapter(repository, mapper);
        verificarDisponibilidadUseCase = new VerificarDisponibilidadUseCase(adapter);
        reservarAsientoUseCase = new ReservarAsientoUseCase(adapter);
        liberarHoldsVencidosUseCase = new LiberarHoldsVencidosUseCase(adapter);
        confirmarOcupacionUseCase = new ConfirmarOcupacionUseCase(adapter);

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

    private UUID insertAsiento(String estado) {
        UUID id = UUID.randomUUID();
        databaseClient.sql(
                        "INSERT INTO asientos(id, fila, columna, numero, zona_id, estado, version) VALUES ($1, 1, 1, 'A1', $2, $3, 0)")
                .bind(0, id)
                .bind(1, zonaId)
                .bind(2, estado)
                .fetch().rowsUpdated().block();
        return id;
    }

    private UUID insertAsientoConExpiraEn(String estado, LocalDateTime expiraEn) {
        UUID id = UUID.randomUUID();
        databaseClient.sql(
                        "INSERT INTO asientos(id, fila, columna, numero, zona_id, estado, version, expira_en) VALUES ($1, 1, 1, 'A1', $2, $3, 0, $4)")
                .bind(0, id)
                .bind(1, zonaId)
                .bind(2, estado)
                .bind(3, expiraEn)
                .fetch().rowsUpdated().block();
        return id;
    }

    // T011: verificación con asientos en distintos estados sobre PostgreSQL real
    @Test
    void verificaDisponibilidadSobrePostgresReal() {
        UUID idDisponible = insertAsiento("DISPONIBLE");
        UUID idReservado = insertAsiento("RESERVADO");
        UUID idOcupado = insertAsiento("OCUPADO");

        StepVerifier.create(verificarDisponibilidadUseCase.ejecutar(idDisponible))
                .assertNext(a -> assertEquals(EstadoAsiento.DISPONIBLE, a.getEstado()))
                .verifyComplete();

        StepVerifier.create(verificarDisponibilidadUseCase.ejecutar(idReservado))
                .assertNext(a -> assertEquals(EstadoAsiento.RESERVADO, a.getEstado()))
                .verifyComplete();

        StepVerifier.create(verificarDisponibilidadUseCase.ejecutar(idOcupado))
                .assertNext(a -> assertEquals(EstadoAsiento.OCUPADO, a.getEstado()))
                .verifyComplete();
    }

    // T019: reservar → verificar expiraEn en BD → scheduler libera hold
    @Test
    void reservarVerificaExpiraEnYSchedulerLiberaHold() {
        UUID id = insertAsiento("DISPONIBLE");

        StepVerifier.create(reservarAsientoUseCase.ejecutar(id))
                .assertNext(a -> {
                    assertEquals(EstadoAsiento.RESERVADO, a.getEstado());
                    assertNotNull(a.getExpiraEn());
                    assertTrue(a.getExpiraEn().isAfter(LocalDateTime.now()));
                })
                .verifyComplete();

        databaseClient.sql("UPDATE asientos SET expira_en = $1 WHERE id = $2")
                .bind(0, LocalDateTime.now().minusMinutes(1))
                .bind(1, id)
                .fetch().rowsUpdated().block();

        StepVerifier.create(liberarHoldsVencidosUseCase.ejecutar(LocalDateTime.now()))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(verificarDisponibilidadUseCase.ejecutar(id))
                .assertNext(a -> {
                    assertEquals(EstadoAsiento.DISPONIBLE, a.getEstado());
                    assertNull(a.getExpiraEn());
                })
                .verifyComplete();
    }

    // T027: flujo reservar → confirmar → asiento OCUPADO en BD
    @Test
    void flujoReservarConfirmarResultaEnAsientoOcupado() {
        UUID id = insertAsiento("DISPONIBLE");

        StepVerifier.create(reservarAsientoUseCase.ejecutar(id))
                .assertNext(a -> assertEquals(EstadoAsiento.RESERVADO, a.getEstado()))
                .verifyComplete();

        StepVerifier.create(confirmarOcupacionUseCase.confirmar(id))
                .assertNext(a -> {
                    assertEquals(EstadoAsiento.OCUPADO, a.getEstado());
                    assertNull(a.getExpiraEn());
                })
                .verifyComplete();
    }

    // T033: concurrencia real — dos hilos simultáneos, cero sobreventa
    @Test
    void concurrenciaRealCeroSobreventa() throws InterruptedException {
        UUID id = insertAsiento("DISPONIBLE");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger exitosas = new AtomicInteger(0);
        AtomicInteger fallidas = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            try {
                latch.await();
                reservarAsientoUseCase.ejecutar(id).block();
                exitosas.incrementAndGet();
            } catch (AsientoReservadoPorOtroException e) {
                fallidas.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                latch.await();
                reservarAsientoUseCase.ejecutar(id).block();
                exitosas.incrementAndGet();
            } catch (AsientoReservadoPorOtroException e) {
                fallidas.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();
        latch.countDown();
        t1.join();
        t2.join();

        assertEquals(2, exitosas.get() + fallidas.get(), "Deben sumar exactamente 2 resultados");
        assertEquals(1, exitosas.get(), "Solo una reserva debe ser exitosa");
        assertEquals(1, fallidas.get(), "La segunda debe fallar con AsientoReservadoPorOtroException");
    }

    // T036: edge case — hold expira exactamente durante confirmación
    @Test
    void holdExpiradoAlConfirmarLanzaHoldExpiradoException() {
        UUID id = insertAsientoConExpiraEn("RESERVADO", LocalDateTime.now().minusSeconds(1));

        StepVerifier.create(confirmarOcupacionUseCase.confirmar(id))
                .expectError(HoldExpiradoException.class)
                .verify();
    }
}
