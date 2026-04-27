package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.CambiarEstadoAsientoUseCase;
import com.ticketseller.application.CambiarEstadoMasivoUseCase;
import com.ticketseller.application.ConsultarHistorialAsientoUseCase;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoRequest;
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
        Asiento asientoActualizado = Asiento.builder()
                .id(UUID.randomUUID())
                .fila("1")
                .columna(1)
                .numero("1")
                .estado(EstadoAsiento.MANTENIMIENTO)
                .build();
        when(cambiarEstadoAsientoUseCase.ejecutar(any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(asientoActualizado));

        CambiarEstadoRequest request = new CambiarEstadoRequest(EstadoAsiento.MANTENIMIENTO, "Motivo test");

        webTestClient.patch()
                .uri("/api/eventos/{eventoId}/asientos/{asientoId}/estado", eventoId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
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
