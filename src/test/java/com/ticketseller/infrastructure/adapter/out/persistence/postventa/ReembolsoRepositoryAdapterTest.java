package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketEntity;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.postventa.mapper.ReembolsoPersistenceMapper;
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

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class ReembolsoRepositoryAdapterTest {

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
    private ReembolsoR2dbcRepository repository;
    @Autowired
    private TicketR2dbcRepository ticketRepository;
    @Autowired
    private DatabaseClient databaseClient;

    private ReembolsoRepositoryAdapter adapter;
    private UUID ticketId;
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
        ticketId = UUID.randomUUID();

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
                VALUES(:id, :comprador, :eventoId, 'COMPLETADA', now(), now() + interval '1 day', 100.00)
                """).bind("id", ventaId).bind("comprador", UUID.randomUUID()).bind("eventoId", eventoId)
                .fetch().rowsUpdated().block();
        ticketRepository.save(TicketEntity.builder()
                        .id(ticketId)
                        .ventaId(ventaId)
                        .eventoId(eventoId)
                        .zonaId(zonaId)
                        .estado(EstadoTicket.CANCELADO.name())
                        .precio(BigDecimal.valueOf(100))
                        .esCortesia(false)
                        .build())
                .block();

        adapter = new ReembolsoRepositoryAdapter(repository, Mappers.getMapper(ReembolsoPersistenceMapper.class));
    }

    @Test
    void deberiaGuardarReembolsoPendiente() {
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.valueOf(100))
                .tipo(TipoReembolso.TOTAL)
                .estado(EstadoReembolso.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        StepVerifier.create(adapter.guardar(reembolso))
                .expectNextMatches(saved -> saved.getId().equals(reembolso.getId())
                        && EstadoReembolso.PENDIENTE.equals(saved.getEstado()))
                .verifyComplete();
    }
}

