package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AsientoMantenimientoControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AsientoRepositoryPort asientoRepositoryPort;

    private UUID eventoId;
    private Asiento asiento;

    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();
        asiento = Asiento.builder()
                .id(UUID.randomUUID())
                .fila("1")
                .columna(1)
                .numero("1")
                .estado(EstadoAsiento.DISPONIBLE)
                .build();
    }

    @Test
    void transicionValidaRetorna200() {

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoAsiento.MANTENIMIENTO, "Motivo test");

        webTestClient.patch()
                .uri("/api/eventos/{eventoId}/asientos/{asientoId}/estado", eventoId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void sinEstadoDestinoRetorna400() {
        String invalidRequest = "{\"motivo\": \"Motivo test\"}";

        webTestClient.patch()
                .uri("/api/eventos/{eventoId}/asientos/{asientoId}/estado", eventoId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

}
