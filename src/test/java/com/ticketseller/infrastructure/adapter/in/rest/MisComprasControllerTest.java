package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.postventa.ConsultarEstadoReembolsoUseCase;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MisComprasController.class)
@Import(GlobalExceptionHandler.class)
class MisComprasControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsultarEstadoReembolsoUseCase consultarEstadoReembolsoUseCase;

    @Test
    void listarMisComprasRetorna200() {
        UUID compradorId = UUID.randomUUID();
        TicketConReembolsoResponse response = new TicketConReembolsoResponse(UUID.randomUUID(), null, null, EstadoTicket.CANCELADO, null, null, null, null, null);

        when(consultarEstadoReembolsoUseCase.ejecutar(any())).thenReturn(Flux.just(response));

        webTestClient.get()
                .uri("/api/compras/mis-compras")
                .header("X-Comprador-Id", compradorId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TicketConReembolsoResponse.class)
                .hasSize(1);
    }
}
