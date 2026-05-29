package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.LiquidacionController;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.LiquidacionRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ticketseller.infrastructure.config.TestWebSecurityConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = LiquidacionController.class)
@Import({GlobalExceptionHandler.class, TestWebSecurityConfig.class})
class LiquidacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsultarSnapshotUseCase consultarSnapshotUseCase;

    @MockBean
    private LiquidacionRestMapper liquidacionRestMapper;

    @Test
    void getSnapshotEventoFinalizadoRetorna200ConCondiciones() {
        UUID eventoId = UUID.randomUUID();
        SnapshotLiquidacion snapshot = SnapshotLiquidacion.builder()
                .eventoId(eventoId)
                .condiciones(Map.of(
                        "VENDIDO_SIN_ASISTENCIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("VENDIDO_SIN_ASISTENCIA").cantidad(50).valorTotal(BigDecimal.valueOf(2500000)).build(),
                        "CORTESIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("CORTESIA").cantidad(10).valorTotal(BigDecimal.ZERO).build(),
                        "CANCELADO", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("CANCELADO").cantidad(5).valorTotal(BigDecimal.valueOf(250000)).build()
                ))
                .timestampGeneracion(LocalDateTime.now())
                .build();
        SnapshotLiquidacionResponse response = new SnapshotLiquidacionResponse(
                eventoId,
                null,
                null,
                null,
                List.of(
                        new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 50, BigDecimal.valueOf(2500000), null),
                        new CondicionTicketResponse("CORTESIA", 10, BigDecimal.ZERO, null),
                        new CondicionTicketResponse("CANCELADO", 5, BigDecimal.valueOf(250000), null)
                ),
                snapshot.getTimestampGeneracion()
        );

        when(consultarSnapshotUseCase.ejecutar(eventoId)).thenReturn(Mono.just(snapshot));
        when(liquidacionRestMapper.toSnapshotResponse(snapshot)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/snapshot", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eventoId").isEqualTo(eventoId.toString())
                .jsonPath("$.condiciones").isArray()
                .jsonPath("$.condiciones.length()").isEqualTo(3);
    }

    @Test
    void getSnapshotEventoActivoRetorna409() {
        UUID eventoId = UUID.randomUUID();

        when(consultarSnapshotUseCase.ejecutar(eventoId))
                .thenReturn(Mono.error(new EventoNoFinalizadoException("Evento no finalizado")));

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/snapshot", eventoId)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getSnapshotTodosValidadosRetornaCeroEnDemasCondiciones() {
        UUID eventoId = UUID.randomUUID();
        SnapshotLiquidacion snapshot = SnapshotLiquidacion.builder()
                .eventoId(eventoId)
                .condiciones(Map.of(
                        "VENDIDO_SIN_ASISTENCIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("VENDIDO_SIN_ASISTENCIA").cantidad(100).valorTotal(BigDecimal.valueOf(5000000)).build()
                ))
                .timestampGeneracion(LocalDateTime.now())
                .build();
        SnapshotLiquidacionResponse response = new SnapshotLiquidacionResponse(
                eventoId,
                null,
                null,
                null,
                List.of(new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 100, BigDecimal.valueOf(5000000), null)),
                snapshot.getTimestampGeneracion()
        );

        when(consultarSnapshotUseCase.ejecutar(eventoId)).thenReturn(Mono.just(snapshot));
        when(liquidacionRestMapper.toSnapshotResponse(snapshot)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/snapshot", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.condiciones.length()").isEqualTo(1)
                .jsonPath("$.condiciones[0].condicion").isEqualTo("VENDIDO_SIN_ASISTENCIA");
    }

    @Test
    void getSnapshotDiferenciaCortesiaDeRegulares() {
        UUID eventoId = UUID.randomUUID();
        SnapshotLiquidacion snapshot = SnapshotLiquidacion.builder()
                .eventoId(eventoId)
                .condiciones(Map.of(
                        "VENDIDO_SIN_ASISTENCIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("VENDIDO_SIN_ASISTENCIA").cantidad(80).valorTotal(BigDecimal.valueOf(4000000)).build(),
                        "CORTESIA", SnapshotLiquidacion.CondicionLiquidacion.builder()
                                .condicion("CORTESIA").cantidad(20).valorTotal(BigDecimal.ZERO).build()
                ))
                .timestampGeneracion(LocalDateTime.now())
                .build();
        SnapshotLiquidacionResponse response = new SnapshotLiquidacionResponse(
                eventoId,
                null,
                null,
                null,
                List.of(
                        new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 80, BigDecimal.valueOf(4000000), null),
                        new CondicionTicketResponse("CORTESIA", 20, BigDecimal.ZERO, null)
                ),
                snapshot.getTimestampGeneracion()
        );

        when(consultarSnapshotUseCase.ejecutar(eventoId)).thenReturn(Mono.just(snapshot));
        when(liquidacionRestMapper.toSnapshotResponse(snapshot)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/snapshot", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.condiciones.length()").isEqualTo(2);
    }

    @Test
    void getSnapshotEventoInexistenteRetorna404() {
        UUID eventoId = UUID.randomUUID();

        when(consultarSnapshotUseCase.ejecutar(eventoId))
                .thenReturn(Mono.error(new EventoNotFoundException("Evento no encontrado")));

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/snapshot", eventoId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
