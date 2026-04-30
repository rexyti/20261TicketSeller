package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper.CodigoPromocionalPersistenceMapper;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class CodigoPromocionalRepositoryAdapterTest {

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
    private CodigoPromocionalR2dbcRepository repository;

    @Autowired
    private PromocionR2dbcRepository promocionR2dbcRepository;

    @Autowired
    private EventoR2dbcRepository eventoR2dbcRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private CodigoPromocionalRepositoryAdapter adapter;
    private PromocionRepositoryAdapter promocionAdapter;
    private EventoRepositoryAdapter eventoAdapter;

    private UUID eventoId;
    private UUID promocionId;

    @BeforeEach
    void setup() throws Exception {
        CodigoPromocionalPersistenceMapper mapper = Mappers.getMapper(CodigoPromocionalPersistenceMapper.class);
        adapter = new CodigoPromocionalRepositoryAdapter(repository, mapper);

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

        promocionId = UUID.randomUUID();
        Promocion promocion = Promocion.builder()
                .id(promocionId)
                .nombre("Codigos 50")
                .tipo(TipoPromocion.CODIGOS)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .build();
        promocionAdapter.guardar(promocion).block();
    }

    @Test
    void deberiaGuardarTodosYBuscarPorCodigo() {
        CodigoPromocional c1 = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("PROMO-A1")
                .promocionId(promocionId)
                .usosMaximos(1)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        CodigoPromocional c2 = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("PROMO-A2")
                .promocionId(promocionId)
                .usosMaximos(1)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        StepVerifier.create(adapter.guardarTodos(List.of(c1, c2)).collectList())
                .expectNextMatches(list -> list.size() == 2)
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorCodigo("PROMO-A1"))
                .expectNextMatches(found -> found.getId().equals(c1.getId()))
                .verifyComplete();
    }

    @Test
    void deberiaIncrementarUsoAtomicoExitosamente() {
        CodigoPromocional c1 = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("PROMO-ATOMICO")
                .promocionId(promocionId)
                .usosMaximos(2)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        adapter.guardarTodos(List.of(c1)).blockLast();

        StepVerifier.create(adapter.incrementarUsoAtomico("PROMO-ATOMICO", LocalDateTime.now()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorCodigo("PROMO-ATOMICO"))
                .expectNextMatches(found -> found.getUsosActuales() == 1)
                .verifyComplete();
    }

    @Test
    void noDeberiaIncrementarSiAlcanzoMaximo() {
        CodigoPromocional c1 = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("PROMO-MAX")
                .promocionId(promocionId)
                .usosMaximos(1)
                .usosActuales(1)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        adapter.guardarTodos(List.of(c1)).blockLast();

        StepVerifier.create(adapter.incrementarUsoAtomico("PROMO-MAX", LocalDateTime.now()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void noDeberiaIncrementarSiExpiro() {
        CodigoPromocional c1 = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("PROMO-EXPIRED")
                .promocionId(promocionId)
                .usosMaximos(5)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now().minusDays(5))
                .fechaFin(LocalDateTime.now().minusDays(1))
                .estado(EstadoCodigoPromocional.EXPIRADO)
                .build();

        adapter.guardarTodos(List.of(c1)).blockLast();

        StepVerifier.create(adapter.incrementarUsoAtomico("PROMO-EXPIRED", LocalDateTime.now()))
                .expectNext(false)
                .verifyComplete();
    }
}
