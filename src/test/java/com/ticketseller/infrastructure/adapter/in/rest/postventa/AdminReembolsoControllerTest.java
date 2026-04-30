package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.GestionarReembolsoManualUseCase;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoManualRequest;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AdminReembolsoController.class)
@Import(GlobalExceptionHandler.class)
class AdminReembolsoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GestionarReembolsoManualUseCase gestionarReembolsoManualUseCase;
    @MockBean
    private PostVentaRestMapper postVentaRestMapper;

    @Test
    void reembolsoManualTotalRetorna200() {
        UUID ticketId = UUID.randomUUID();
        ReembolsoManualRequest request = new ReembolsoManualRequest(TipoReembolso.TOTAL, null, UUID.randomUUID());
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .estado(EstadoReembolso.COMPLETADO)
                .monto(BigDecimal.valueOf(100))
                .fechaCompletado(LocalDateTime.now())
                .build();
        ReembolsoResponse response = new ReembolsoResponse(reembolso.getId(), reembolso.getEstado(), reembolso.getMonto(),
                reembolso.getAgenteId(), reembolso.getFechaCompletado());
        when(gestionarReembolsoManualUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(reembolso));
        when(postVentaRestMapper.toReembolsoResponse(reembolso)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/admin/tickets/{id}/reembolso", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("COMPLETADO");
    }

    @Test
    void reembolsoManualParcialRetorna200() {
        UUID ticketId = UUID.randomUUID();
        ReembolsoManualRequest request = new ReembolsoManualRequest(TipoReembolso.PARCIAL, BigDecimal.valueOf(50), UUID.randomUUID());
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .estado(EstadoReembolso.COMPLETADO)
                .monto(BigDecimal.valueOf(50))
                .fechaCompletado(LocalDateTime.now())
                .build();
        ReembolsoResponse response = new ReembolsoResponse(reembolso.getId(), reembolso.getEstado(), reembolso.getMonto(),
                reembolso.getAgenteId(), reembolso.getFechaCompletado());
        when(gestionarReembolsoManualUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(reembolso));
        when(postVentaRestMapper.toReembolsoResponse(reembolso)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/admin/tickets/{id}/reembolso", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void procesarColaAutomaticaRetorna200() {
        when(gestionarReembolsoManualUseCase.procesarColaPendiente()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/v1/admin/reembolsos/procesar-cola")
                .exchange()
                .expectStatus().isOk();
    }
}

