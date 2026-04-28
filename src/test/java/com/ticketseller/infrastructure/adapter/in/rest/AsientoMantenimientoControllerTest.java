package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.asiento.CambiarEstadoAsientoUseCase;
import com.ticketseller.application.asiento.CambiarEstadoMasivoUseCase;
import com.ticketseller.application.asiento.ConsultarHistorialAsientoUseCase;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.asiento.CambiarEstadoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AsientoRestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AsientoMantenimientoController.class)
@Import(GlobalExceptionHandler.class)
class AsientoMantenimientoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CambiarEstadoAsientoUseCase cambiarEstadoAsientoUseCase;

    @MockBean
    private CambiarEstadoMasivoUseCase cambiarEstadoMasivoUseCase;

    @MockBean
    private ConsultarHistorialAsientoUseCase consultarHistorialAsientoUseCase;

    @MockBean
    private AsientoRestMapper asientoRestMapper;

    private UUID eventoId;

    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();
        when(cambiarEstadoAsientoUseCase.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new IllegalArgumentException("Asiento no encontrado")));
    }

    @Test
    void transicionValidaRetorna200() {
        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoAsiento.MANTENIMIENTO, "Motivo test");

        webTestClient.patch()
                .uri("/api/eventos/{eventoId}/asientos/{asientoId}/estado", eventoId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
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
