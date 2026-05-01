package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.checkout.ConsultarEstadoTicketUseCase;
import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.CategoriaTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.TicketConsultaController;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.dto.TicketEstadoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AccesoRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = TicketConsultaController.class)
@Import(GlobalExceptionHandler.class)
class TicketConsultaControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private ConsultarEstadoTicketUseCase consultarEstadoTicketUseCase;
    @MockBean
    private AccesoRestMapper accesoRestMapper;

    @Test
    void consultarEstado_ok() {
        UUID ticketId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDateTime fechaEvento = LocalDateTime.now().plusDays(1);
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .eventoId(eventoId)
                .estado(EstadoTicket.VENDIDO)
                .accessDetails(AccessDetails.builder()
                        .categoria(CategoriaTicket.VIP)
                        .zona("A")
                        .compuerta("NORTE-1")
                        .fechaEvento(fechaEvento)
                        .build())
                .build();
        TicketEstadoResponse response = new TicketEstadoResponse(
                ticketId,
                eventoId,
                EstadoTicket.VENDIDO,
                "VIP",
                "A",
                "NORTE-1",
                fechaEvento
        );
        when(consultarEstadoTicketUseCase.ejecutar(ticketId)).thenReturn(Mono.just(ticket));
        when(accesoRestMapper.toResponse(ticket)).thenReturn(response);
        webTestClient.get()
                .uri("/api/tickets/{id}", ticketId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticketId").isEqualTo(ticketId.toString())
                .jsonPath("$.estado").isEqualTo("VENDIDO")
                .jsonPath("$.categoria").isEqualTo("VIP")
                .jsonPath("$.bloque").isEqualTo("A")
                .jsonPath("$.coordenadaAcceso").isEqualTo("NORTE-1")
                .jsonPath("$.eventoId").isEqualTo(eventoId.toString())
                .jsonPath("$.fechaEvento").isNotEmpty();
    }
}
