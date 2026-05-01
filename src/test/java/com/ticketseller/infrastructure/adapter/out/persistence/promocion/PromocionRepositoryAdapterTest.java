package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Integración — requiere Docker")
@Testcontainers
@DataR2dbcTest
class PromocionRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ticketseller")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/ticketseller");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private PromocionRepositoryPort promocionRepositoryPort;

    @Test
    void deberiaCrearYRestaurarPreventaConRestriccionPorTipoUsuario() {
        UUID eventoId = UUID.randomUUID();
        Promocion preventa = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Preventa VIP")
                .tipo(TipoPromocion.PREVENTA)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(7))
                .estado(EstadoPromocion.ACTIVA)
                .tipoUsuarioRestringido(TipoUsuario.VIP)
                .build();

        StepVerifier.create(promocionRepositoryPort.guardar(preventa)
                        .flatMap(saved -> promocionRepositoryPort.buscarPorId(saved.getId())))
                .assertNext(loaded -> {
                    assertThat(loaded.getTipoUsuarioRestringido()).isEqualTo(TipoUsuario.VIP);
                    assertThat(loaded.getEstado()).isEqualTo(EstadoPromocion.ACTIVA);
                })
                .verifyComplete();
    }
}
