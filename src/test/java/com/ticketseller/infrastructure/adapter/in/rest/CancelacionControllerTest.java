package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.CancelarTicketUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CancelacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CancelarTicketRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CancelacionController.class)
@Import(GlobalExceptionHandler.class)
class CancelacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CancelarTicketUseCase cancelarTicketUseCase;

    @Test
    void cancelarTicketIndividualRetorna200() {
        UUID ticketId = UUID.randomUUID();
        CancelacionResponse response = new CancelacionResponse(List.of(ticketId), UUID.randomUUID(), BigDecimal.valueOf(100));

        when(cancelarTicketUseCase.ejecutar(anyList())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/tickets/{id}/cancelar", ticketId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.reembolsoId").exists();
    }

    @Test
    void cancelarParcialRetorna200() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        CancelarTicketRequest request = new CancelarTicketRequest(List.of(t1, t2));
        CancelacionResponse response = new CancelacionResponse(List.of(t1, t2), UUID.randomUUID(), BigDecimal.valueOf(200));

        when(cancelarTicketUseCase.ejecutar(anyList())).thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/api/tickets/cancelar-parcial")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.montoPendiente").isEqualTo(200);
    }
}
