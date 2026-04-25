package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.CambiarEstadoTicketUseCase;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TicketRestMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AdminTicketController.class)
@Import(GlobalExceptionHandler.class)
class AdminTicketControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CambiarEstadoTicketUseCase cambiarEstadoTicketUseCase;

    @MockBean
    private ReembolsoRepositoryPort reembolsoRepositoryPort;

    @MockBean
    private TicketRestMapper ticketRestMapper;

    @Test
    void cambiarEstadoRetorna200() {
        UUID ticketId = UUID.randomUUID();
        CambiarEstadoTicketRequest request = new CambiarEstadoTicketRequest(EstadoTicket.ANULADO, "Fraude detectado");
        Ticket ticket = Ticket.builder().id(ticketId).estado(EstadoTicket.ANULADO).build();

        when(cambiarEstadoTicketUseCase.ejecutar(eq(ticketId), eq(EstadoTicket.ANULADO), eq("Fraude detectado"), any()))
                .thenReturn(Mono.just(ticket));
        when(reembolsoRepositoryPort.findByTicketId(ticketId)).thenReturn(Mono.empty());
        when(ticketRestMapper.toResponse(any(), any())).thenReturn(new TicketConReembolsoResponse(ticketId, null, null, EstadoTicket.ANULADO, null, null, null, null, null));
        
        webTestClient.patch()
                .uri("/api/admin/tickets/{id}/estado", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("ANULADO");
    }
}
