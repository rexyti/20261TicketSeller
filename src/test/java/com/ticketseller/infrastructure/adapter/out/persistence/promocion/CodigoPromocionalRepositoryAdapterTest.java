package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Integración — requiere Docker")
@Testcontainers
@DataR2dbcTest
class CodigoPromocionalRepositoryAdapterTest {

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
    private CodigoPromocionalRepositoryPort codigoRepositoryPort;

    @Test
    void deberiaIncrementarUsosActualesEnBD() {
        UUID promocionId = UUID.randomUUID();
        CodigoPromocional codigo = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("TEST-INCR")
                .promocionId(promocionId)
                .usosMaximos(5)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        StepVerifier.create(codigoRepositoryPort.guardarTodos(List.of(codigo))
                        .then(codigoRepositoryPort.incrementarUsos(codigo.getId()))
                        .flatMap(updated -> codigoRepositoryPort.buscarPorCodigo("TEST-INCR")))
                .assertNext(loaded -> assertThat(loaded.getUsosActuales()).isEqualTo(1))
                .verifyComplete();
    }
}
