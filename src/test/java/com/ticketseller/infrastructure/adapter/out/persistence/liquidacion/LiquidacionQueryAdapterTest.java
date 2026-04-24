package com.ticketseller.infrastructure.adapter.out.persistence.liquidacion;

import com.ticketseller.domain.model.SnapshotLiquidacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class LiquidacionQueryAdapterTest {

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
    private DatabaseClient databaseClient;

    private LiquidacionQueryAdapter adapter;

    @BeforeEach
    void setup() throws Exception {
        adapter = new LiquidacionQueryAdapter(databaseClient);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();
    }

    @Test
    void deberiaAgregarTicketsDeMultiplesCondicionesEnSnapshot() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, ventaId);

        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", false, BigDecimal.valueOf(50000));
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", false, BigDecimal.valueOf(50000));
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", true, BigDecimal.ZERO);
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "ANULADO", false, BigDecimal.valueOf(50000));

        StepVerifier.create(adapter.obtenerSnapshotPorEvento(eventoId))
                .assertNext(snapshot -> {
                    assert snapshot.getEventoId().equals(eventoId);
                    assert snapshot.getCondiciones().containsKey("VENDIDO_SIN_ASISTENCIA");
                    assert snapshot.getCondiciones().containsKey("CORTESIA");
                    assert snapshot.getCondiciones().containsKey("CANCELADO");

                    SnapshotLiquidacion.CondicionLiquidacion vendidos = snapshot.getCondiciones().get("VENDIDO_SIN_ASISTENCIA");
                    assert vendidos.getCantidad() == 2;
                    assert vendidos.getValorTotal().compareTo(BigDecimal.valueOf(100000)) == 0;

                    SnapshotLiquidacion.CondicionLiquidacion cortesias = snapshot.getCondiciones().get("CORTESIA");
                    assert cortesias.getCantidad() == 1;

                    SnapshotLiquidacion.CondicionLiquidacion cancelados = snapshot.getCondiciones().get("CANCELADO");
                    assert cancelados.getCantidad() == 1;
                })
                .verifyComplete();
    }

    @Test
    void deberiaRetornarRecaudoConCancelacionesDescontadas() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();

        insertarDatosBase(recintoId, zonaId, eventoId, ventaId);

        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", false, BigDecimal.valueOf(100000));
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", false, BigDecimal.valueOf(100000));
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "VENDIDO", true, BigDecimal.ZERO);
        insertarTicket(UUID.randomUUID(), ventaId, eventoId, zonaId, "ANULADO", false, BigDecimal.valueOf(100000));

        StepVerifier.create(adapter.obtenerRecaudoPorEvento(eventoId))
                .assertNext(recaudo -> {
                    assert recaudo.get("recaudoRegular").compareTo(BigDecimal.valueOf(200000)) == 0;
                    assert recaudo.get("recaudoCortesia").compareTo(BigDecimal.ZERO) == 0;
                    assert recaudo.get("cancelaciones").compareTo(BigDecimal.valueOf(100000)) == 0;
                    assert recaudo.get("recaudoNeto").compareTo(BigDecimal.valueOf(100000)) == 0;
                })
                .verifyComplete();
    }

    private void insertarDatosBase(UUID recintoId, UUID zonaId, UUID eventoId, UUID ventaId) {
        databaseClient.sql("""
                        INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion,
                        compuertas_ingreso, activo)
                        VALUES ($1, 'Arena Test', 'Bogota', 'Calle 1', 5000, '300123', now(), 2, true)
                        """)
                .bind(0, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("INSERT INTO zonas(id, recinto_id, nombre, capacidad) VALUES ($1, $2, 'Platea', 100)")
                .bind(0, zonaId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO eventos(id, nombre, fecha_inicio, fecha_fin, tipo, recinto_id, estado)
                        VALUES ($1, 'Concierto Test', now(), now() + interval '2 hours', 'MUSICAL', $2, 'FINALIZADO')
                        """)
                .bind(0, eventoId)
                .bind(1, recintoId)
                .fetch().rowsUpdated().block();

        databaseClient.sql("""
                        INSERT INTO ventas(id, comprador_id, evento_id, estado, fecha_creacion, fecha_expiracion, total)
                        VALUES ($1, $2, $3, 'COMPLETADA', now(), now() + interval '15 minutes', 200000)
                        """)
                .bind(0, ventaId)
                .bind(1, UUID.randomUUID())
                .bind(2, eventoId)
                .fetch().rowsUpdated().block();
    }

    private void insertarTicket(UUID ticketId, UUID ventaId, UUID eventoId, UUID zonaId,
                                String estado, boolean esCortesia, BigDecimal precio) {
        databaseClient.sql("""
                        INSERT INTO tickets(id, venta_id, evento_id, zona_id, estado, precio, es_cortesia)
                        VALUES ($1, $2, $3, $4, $5, $6, $7)
                        """)
                .bind(0, ticketId)
                .bind(1, ventaId)
                .bind(2, eventoId)
                .bind(3, zonaId)
                .bind(4, estado)
                .bind(5, precio)
                .bind(6, esCortesia)
                .fetch().rowsUpdated().block();
    }
}
