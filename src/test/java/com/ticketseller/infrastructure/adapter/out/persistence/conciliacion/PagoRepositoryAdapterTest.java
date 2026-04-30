package com.ticketseller.infrastructure.adapter.out.persistence.conciliacion;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.PagoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.PagoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.mapper.PagoPersistenceMapper;
import org.junit.jupiter.api.Assertions;
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

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class PagoRepositoryAdapterTest {

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
    private PagoR2dbcRepository repository;
    @Autowired
    private DatabaseClient databaseClient;

    private PagoRepositoryAdapter adapter;
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

        adapter = new PagoRepositoryAdapter(repository, Mappers.getMapper(PagoPersistenceMapper.class));
    }

    @Test
    void deberiaGuardarYRecuperarPagoPorVentaId() {
        Pago pago = pagoBuilder(EstadoConciliacion.VERIFICADO).build();
        adapter.guardar(pago).block();

        StepVerifier.create(adapter.buscarPorVentaId(ventaId))
                .assertNext(p -> {
                    assert p.getEstado() == EstadoConciliacion.VERIFICADO;
                    assert p.getVentaId().equals(ventaId);
                })
                .verifyComplete();
    }

    @Test
    void deberiaEncontrarPagoPorIdExterno() {
        String idExterno = "ext-gateway-" + UUID.randomUUID();
        Pago pago = pagoBuilder(EstadoConciliacion.PENDIENTE).idExternoPasarela(idExterno).build();
        adapter.guardar(pago).block();

        StepVerifier.create(adapter.buscarPorIdExterno(idExterno))
                .assertNext(p -> Assertions.assertEquals(idExterno, p.getIdExternoPasarela()))
                .verifyComplete();
    }

    @Test
    void deberiaActualizarEstadoDelPago() {
        Pago pago = pagoBuilder(EstadoConciliacion.VERIFICADO).build();
        Pago guardado = adapter.guardar(pago).block();

        StepVerifier.create(adapter.actualizarEstado(guardado.getId(), EstadoConciliacion.CONFIRMADO))
                .assertNext(p -> Assertions.assertEquals(EstadoConciliacion.CONFIRMADO, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaListarPagosPendientesAntiguos() {
        Pago pagoAntiguo = pagoBuilder(EstadoConciliacion.PENDIENTE)
                .fechaCreacion(LocalDateTime.now().minusMinutes(20))
                .fechaActualizacion(LocalDateTime.now().minusMinutes(20))
                .build();
        adapter.guardar(pagoAntiguo).block();

        StepVerifier.create(adapter.buscarPendientesAnterioresA(LocalDateTime.now().minusMinutes(15)))
                .assertNext(p -> Assertions.assertEquals(EstadoConciliacion.PENDIENTE, p.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaNoRetornarPagosPendientesRecientes() {
        Pago pagoReciente = pagoBuilder(EstadoConciliacion.PENDIENTE).build();
        adapter.guardar(pagoReciente).block();

        StepVerifier.create(adapter.buscarPendientesAnterioresA(LocalDateTime.now().minusMinutes(15)))
                .verifyComplete();
    }

    @Test
    void deberiaNoPermitirDuplicadosPorIdExterno() {
        String idExterno = "ext-dup-" + UUID.randomUUID();
        Pago pago1 = pagoBuilder(EstadoConciliacion.PENDIENTE).idExternoPasarela(idExterno).build();
        Pago pago2 = pagoBuilder(EstadoConciliacion.PENDIENTE).idExternoPasarela(idExterno).build();
        adapter.guardar(pago1).block();

        StepVerifier.create(adapter.guardar(pago2))
                .expectError()
                .verify();
    }

    private Pago.PagoBuilder pagoBuilder(EstadoConciliacion estado) {
        return Pago.builder()
                .id(UUID.randomUUID())
                .ventaId(ventaId)
                .montoEsperado(BigDecimal.valueOf(100))
                .montoPasarela(BigDecimal.valueOf(100))
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now());
    }
}
