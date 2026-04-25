package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.postventa.GestionarReembolsoManualUseCase;
import com.ticketseller.domain.model.EstadoReembolso;
import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.domain.model.TipoReembolso;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ReembolsoManualRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AdminReembolsoController.class)
@Import(GlobalExceptionHandler.class)
class AdminReembolsoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GestionarReembolsoManualUseCase gestionarReembolsoManualUseCase;

    @Test
    void procesarReembolsoRetorna200() {
        UUID ticketId = UUID.randomUUID();
        ReembolsoManualRequest request = new ReembolsoManualRequest(TipoReembolso.TOTAL, BigDecimal.valueOf(100));
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .estado(EstadoReembolso.COMPLETADO)
                .monto(BigDecimal.valueOf(100))
                .build();

        when(gestionarReembolsoManualUseCase.ejecutar(eq(ticketId), eq(TipoReembolso.TOTAL), any()))
                .thenReturn(Mono.just(reembolso));

        webTestClient.post()
                .uri("/api/admin/tickets/{id}/reembolso", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("COMPLETADO");
    }

    @Test
    void procesarColaRetorna200() {
        when(gestionarReembolsoManualUseCase.procesarCola()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/admin/tickets/reembolsos/procesar-cola")
                .exchange()
                .expectStatus().isOk();
    }
}
