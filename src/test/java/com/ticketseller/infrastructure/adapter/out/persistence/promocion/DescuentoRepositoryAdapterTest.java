package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.DescuentoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.PromocionPersistenceMapper;
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
import java.util.List;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class DescuentoRepositoryAdapterTest {

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
    private DescuentoR2dbcRepository repository;

    @Autowired
    private PromocionR2dbcRepository promocionR2dbcRepository;

    @Autowired
    private EventoR2dbcRepository eventoR2dbcRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private DescuentoRepositoryAdapter adapter;
    private PromocionRepositoryAdapter promocionAdapter;
    private EventoRepositoryAdapter eventoAdapter;

    private UUID eventoId;
    private UUID promocionActivaId;

    @BeforeEach
    void setup() throws Exception {
        DescuentoPersistenceMapper mapper = Mappers.getMapper(DescuentoPersistenceMapper.class);
        adapter = new DescuentoRepositoryAdapter(repository, mapper);

        PromocionPersistenceMapper promoMapper = Mappers.getMapper(PromocionPersistenceMapper.class);
        promocionAdapter = new PromocionRepositoryAdapter(promocionR2dbcRepository, promoMapper);

        EventoPersistenceMapper eventoMapper = Mappers.getMapper(EventoPersistenceMapper.class);
        eventoAdapter = new EventoRepositoryAdapter(eventoR2dbcRepository, eventoMapper);

        String script = Files.readString(Path.of("src/test/resources/schema.sql"));
        Flux.fromArray(script.split(";"))
                .map(String::trim)
                .filter(sql -> !sql.isBlank())
                .concatMap(sql -> databaseClient.sql(sql).fetch().rowsUpdated())
                .then()
                .block();

        UUID recintoId = UUID.randomUUID();
        databaseClient.sql("""
                INSERT INTO recintos(id, nombre, ciudad, direccion, capacidad_maxima, telefono, fecha_creacion, compuertas_ingreso, activo)
                VALUES($1, 'Arena', 'Bogota', 'Calle 1', 1000, '300', $2, 4, true)
                """)
                .bind(0, recintoId)
                .bind(1, LocalDateTime.now())
                .fetch()
                .rowsUpdated()
                .block();

        eventoId = UUID.randomUUID();
        Evento evento = Evento.builder()
                .id(eventoId)
                .nombre("Concierto Promo")
                .fechaInicio(LocalDateTime.now().plusDays(2))
                .fechaFin(LocalDateTime.now().plusDays(3))
                .tipo("MUSICAL")
                .recintoId(recintoId)
                .estado(EstadoEvento.ACTIVO)
                .build();
        eventoAdapter.guardar(evento).block();

        promocionActivaId = UUID.randomUUID();
        Promocion promocion = Promocion.builder()
                .id(promocionActivaId)
                .nombre("Promo Descuento")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .build();
        promocionAdapter.guardar(promocion).block();
    }

    @Test
    void deberiaGuardarYBuscarPorPromocionId() {
        Descuento descuento = Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(promocionActivaId)
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(BigDecimal.valueOf(15))
                .acumulable(false)
                .build();

        StepVerifier.create(adapter.guardar(descuento))
                .expectNextMatches(saved -> saved.getId().equals(descuento.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorPromocionId(promocionActivaId).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.getFirst().getValor().compareTo(BigDecimal.valueOf(15)) == 0)
                .verifyComplete();
    }

    @Test
    void deberiaBuscarActivosPorEvento() {
        Descuento descuento = Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(promocionActivaId)
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(BigDecimal.valueOf(20))
                .acumulable(true)
                .build();

        adapter.guardar(descuento).block();

        StepVerifier.create(adapter.buscarActivosPorEvento(eventoId, LocalDateTime.now()).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.getFirst().getId().equals(descuento.getId()))
                .verifyComplete();

        // Probar fuera de fecha
        StepVerifier.create(adapter.buscarActivosPorEvento(eventoId, LocalDateTime.now().plusDays(5)).collectList())
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
}
