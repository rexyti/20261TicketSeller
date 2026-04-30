package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
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
import java.util.UUID;

@DataR2dbcTest
@Testcontainers(disabledWithoutDocker = true)
class PromocionRepositoryAdapterTest {

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
    private PromocionR2dbcRepository repository;

    @Autowired
    private EventoR2dbcRepository eventoR2dbcRepository;

    @Autowired
    private DatabaseClient databaseClient;

    private PromocionRepositoryAdapter adapter;
    private EventoRepositoryAdapter eventoAdapter;
    private UUID eventoId;

    @BeforeEach
    void setup() throws Exception {
        PromocionPersistenceMapper mapper = Mappers.getMapper(PromocionPersistenceMapper.class);
        adapter = new PromocionRepositoryAdapter(repository, mapper);

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
    }

    @Test
    void deberiaGuardarYBuscarPorId() {
        Promocion promocion = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Preventa VIP")
                .tipo(TipoPromocion.PREVENTA)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .tipoUsuarioRestringido(TipoUsuario.VIP)
                .build();

        StepVerifier.create(adapter.guardar(promocion))
                .expectNextMatches(saved -> saved.getId().equals(promocion.getId()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorId(promocion.getId()))
                .expectNextMatches(found -> "Preventa VIP".equals(found.getNombre()) && TipoUsuario.VIP.equals(found.getTipoUsuarioRestringido()))
                .verifyComplete();
    }

    @Test
    void deberiaActualizarEstado() {
        Promocion promocion = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Preventa VIP")
                .tipo(TipoPromocion.PREVENTA)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .build();

        adapter.guardar(promocion).block();

        StepVerifier.create(adapter.actualizarEstado(promocion.getId(), EstadoPromocion.PAUSADA))
                .expectNextMatches(updated -> EstadoPromocion.PAUSADA.equals(updated.getEstado()))
                .verifyComplete();

        StepVerifier.create(adapter.buscarPorId(promocion.getId()))
                .expectNextMatches(found -> EstadoPromocion.PAUSADA.equals(found.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaBuscarActivasPorEvento() {
        LocalDateTime ahora = LocalDateTime.now();

        Promocion promoActivaVigente = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Promo 1")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(ahora.minusDays(1))
                .fechaFin(ahora.plusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .build();

        Promocion promoActivaExpirada = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Promo 2")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(ahora.minusDays(5))
                .fechaFin(ahora.minusDays(1))
                .estado(EstadoPromocion.ACTIVA)
                .build();

        Promocion promoPausada = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Promo 3")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(ahora.minusDays(1))
                .fechaFin(ahora.plusDays(1))
                .estado(EstadoPromocion.PAUSADA)
                .build();

        adapter.guardar(promoActivaVigente).block();
        adapter.guardar(promoActivaExpirada).block();
        adapter.guardar(promoPausada).block();

        StepVerifier.create(adapter.buscarActivasPorEvento(eventoId, TipoPromocion.DESCUENTO, ahora).collectList())
                .expectNextMatches(list -> list.size() == 1 && list.getFirst().getId().equals(promoActivaVigente.getId()))
                .verifyComplete();
    }
}
