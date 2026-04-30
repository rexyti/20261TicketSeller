package com.ticketseller.infrastructure.adapter.out.persistence.transaccion;

import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.HistorialEstadoVentaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.HistorialEstadoVentaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.mapper.HistorialEstadoVentaPersistenceMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class HistorialEstadoVentaRepositoryAdapterTest {

    @Container
    @SuppressWarnings("resource")
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
    private HistorialEstadoVentaR2dbcRepository repository;
    @Autowired
    private DatabaseClient databaseClient;

    private HistorialEstadoVentaRepositoryAdapter adapter;
    private UUID ventaId;

    @BeforeEach
    void setup() throws Exception {
        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();

        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        ventaId = UUID.randomUUID();

        databaseClient.sql("""
                INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion, compuertas_ingreso, activo)
                VALUES(:id, 'Arena', 'Bogota', 'Calle 1', 1000, '300', now(), 3, true)
                """).bind("id", recintoId).fetch().rowsUpdated().block();
        databaseClient.sql("""
                INSERT INTO zonas(id, recinto_id, nombre, capacidad) VALUES(:id, :recintoId, 'VIP', 100)
                """).bind("id", zonaId).bind("recintoId", recintoId).fetch().rowsUpdated().block();
        databaseClient.sql("""
                INSERT INTO eventos(id, nombre, fecha_inicio, fecha_fin, tipo, recinto_id, estado)
                VALUES(:id, 'Concierto', now() + interval '1 day', now() + interval '2 day', 'MUSICAL', :recintoId, 'ACTIVO')
                """).bind("id", eventoId).bind("recintoId", recintoId).fetch().rowsUpdated().block();
        databaseClient.sql("""
                INSERT INTO ventas(id, comprador_id, evento_id, estado, fecha_creacion, fecha_expiracion, total)
                VALUES(:id, :comprador, :eventoId, 'RESERVADA', now(), now() + interval '15 min', 100.00)
                """).bind("id", ventaId).bind("comprador", UUID.randomUUID()).bind("eventoId", eventoId)
                .fetch().rowsUpdated().block();

        adapter = new HistorialEstadoVentaRepositoryAdapter(repository,
                Mappers.getMapper(HistorialEstadoVentaPersistenceMapper.class));
    }

    @Test
    void deberiaGuardarYRecuperarHistorialOrdenado() {
        HistorialEstadoVenta historial1 = HistorialEstadoVenta.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .estadoAnterior(EstadoVenta.PENDIENTE).estadoNuevo(EstadoVenta.RESERVADA)
                .justificacion("reserva de asientos")
                .fechaCambio(LocalDateTime.now().minusMinutes(5))
                .build();
        HistorialEstadoVenta historial2 = HistorialEstadoVenta.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .estadoAnterior(EstadoVenta.RESERVADA).estadoNuevo(EstadoVenta.COMPLETADA)
                .justificacion("pago confirmado")
                .fechaCambio(LocalDateTime.now())
                .build();

        adapter.guardar(historial1).block();
        adapter.guardar(historial2).block();

        StepVerifier.create(adapter.buscarPorVentaId(ventaId))
                .assertNext(h -> {
                    assertEquals(EstadoVenta.PENDIENTE, h.getEstadoAnterior());
                    assertEquals(EstadoVenta.RESERVADA, h.getEstadoNuevo());
                })
                .assertNext(h -> {
                    assertEquals(EstadoVenta.RESERVADA, h.getEstadoAnterior());
                    assertEquals(EstadoVenta.COMPLETADA, h.getEstadoNuevo());
                })
                .verifyComplete();
    }

    @Test
    void deberiaRetornarFluxVacioSiNoHayHistorial() {
        StepVerifier.create(adapter.buscarPorVentaId(ventaId))
                .verifyComplete();
    }
}
