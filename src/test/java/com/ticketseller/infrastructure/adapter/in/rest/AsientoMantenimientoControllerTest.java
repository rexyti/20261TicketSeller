package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

@WebFluxTest(controllers = AsientoMantenimientoController.class)
@Import(GlobalExceptionHandler.class)
class AsientoMantenimientoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    private UUID eventoId;

    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();
        Asiento asiento = Asiento.builder()
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
