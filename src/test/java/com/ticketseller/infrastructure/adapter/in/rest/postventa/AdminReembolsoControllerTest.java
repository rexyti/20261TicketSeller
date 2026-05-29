package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.ConsultarColaReembolsosUseCase;
import com.ticketseller.application.postventa.ProcesarReembolsoManualUseCase;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.config.TestWebSecurityConfig;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoManualRequest;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoPendienteResponse;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AdminReembolsoController.class)
@Import({GlobalExceptionHandler.class, TestWebSecurityConfig.class})
class AdminReembolsoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProcesarReembolsoManualUseCase procesarReembolsoManualUseCase;
    @MockBean
    private ConsultarColaReembolsosUseCase consultarColaReembolsosUseCase;
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
        when(procesarReembolsoManualUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(reembolso));
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
        when(procesarReembolsoManualUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(reembolso));
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
        when(procesarReembolsoManualUseCase.procesarColaPendiente()).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/v1/admin/reembolsos/procesar-cola")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void obtenerColaVaciaRetorna200ConListaVacia() {
        when(consultarColaReembolsosUseCase.ejecutar()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/admin/reembolsos/cola")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReembolsoPendienteResponse.class)
                .hasSize(0);
    }

    @Test
    void obtenerColaConReembolsosPendientesRetorna200ConDatos() {
        UUID reembolsoId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        Reembolso reembolso = Reembolso.builder()
                .id(reembolsoId)
                .ticketId(ticketId)
                .ventaId(ventaId)
                .monto(BigDecimal.valueOf(100))
                .tipo(TipoReembolso.TOTAL)
                .estado(EstadoReembolso.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();
        ReembolsoPendienteResponse response = new ReembolsoPendienteResponse(
                reembolsoId, ticketId, ventaId, BigDecimal.valueOf(100),
                TipoReembolso.TOTAL, EstadoReembolso.PENDIENTE, reembolso.getFechaSolicitud(), null);

        when(consultarColaReembolsosUseCase.ejecutar()).thenReturn(Flux.just(reembolso));
        when(postVentaRestMapper.toReembolsoPendienteResponse(reembolso)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/admin/reembolsos/cola")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ReembolsoPendienteResponse.class)
                .hasSize(1);
    }
}

