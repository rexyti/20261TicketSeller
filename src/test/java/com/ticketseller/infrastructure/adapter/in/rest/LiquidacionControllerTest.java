package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.liquidacion.ConfigurarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.LiquidacionNoConfiguradaException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import com.ticketseller.domain.model.recinto.ConfiguracionLiquidacion;
import com.ticketseller.domain.model.recinto.ModeloNegocio;
import com.ticketseller.domain.model.evento.SnapshotLiquidacion;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.ModeloNegocioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.LiquidacionController;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.LiquidacionRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = LiquidacionController.class)
@Import(GlobalExceptionHandler.class)
class LiquidacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsultarSnapshotUseCase consultarSnapshotUseCase;

    @MockBean
    private ConsultarModeloNegocioUseCase consultarModeloNegocioUseCase;

    @MockBean
    private ConfigurarModeloNegocioUseCase configurarModeloNegocioUseCase;

    @MockBean
    private ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase;

    @MockBean
    private LiquidacionRestMapper liquidacionRestMapper;

    // ======================== US2: Modelo de Negocio ========================

    @Test
    void getModeloNegocioTarifaPlanaRetorna200ConMontoFijo() {
        UUID recintoId = UUID.randomUUID();
        ConfiguracionLiquidacion config = ConfiguracionLiquidacion.builder()
                .recintoId(recintoId)
                .modeloNegocio(ModeloNegocio.TARIFA_PLANA)
                .montoFijo(BigDecimal.valueOf(5000))
                .build();
        ModeloNegocioResponse response = new ModeloNegocioResponse(
                recintoId,
                ModeloNegocio.TARIFA_PLANA,
                null,
                BigDecimal.valueOf(5000)
        );

        when(consultarModeloNegocioUseCase.ejecutar(recintoId)).thenReturn(Mono.just(config));
        when(liquidacionRestMapper.toModeloNegocioResponse(config)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos/{id}/modelo-negocio", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.modelo").isEqualTo("TARIFA_PLANA")
                .jsonPath("$.montoFijo").isEqualTo(5000);
    }

    @Test
    void getModeloNegocioRepartoIngresosRetorna200ConTipoRecinto() {
        UUID recintoId = UUID.randomUUID();
        ConfiguracionLiquidacion config = ConfiguracionLiquidacion.builder()
                .recintoId(recintoId)
                .modeloNegocio(ModeloNegocio.REPARTO_INGRESOS)
                .tipoRecinto(CategoriaRecinto.ESTADIO)
                .build();
        ModeloNegocioResponse response = new ModeloNegocioResponse(
                recintoId,
                ModeloNegocio.REPARTO_INGRESOS,
                CategoriaRecinto.ESTADIO,
                null
        );

        when(consultarModeloNegocioUseCase.ejecutar(recintoId)).thenReturn(Mono.just(config));
        when(liquidacionRestMapper.toModeloNegocioResponse(config)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos/{id}/modelo-negocio", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.modelo").isEqualTo("REPARTO_INGRESOS")
                .jsonPath("$.tipoRecinto").isEqualTo("ESTADIO");
    }

    @Test
    void getModeloNegocioSinConfiguracionRetorna422() {
        UUID recintoId = UUID.randomUUID();

        when(consultarModeloNegocioUseCase.ejecutar(recintoId))
                .thenReturn(Mono.error(new LiquidacionNoConfiguradaException("No configurado")));

        webTestClient.get()
                .uri("/api/v1/recintos/{id}/modelo-negocio", recintoId)
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void getModeloNegocioRecintoInexistenteRetorna404() {
        UUID recintoId = UUID.randomUUID();

        when(consultarModeloNegocioUseCase.ejecutar(recintoId))
                .thenReturn(Mono.error(new RecintoNotFoundException("Recinto no encontrado")));

        webTestClient.get()
                .uri("/api/v1/recintos/{id}/modelo-negocio", recintoId)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ======================== US1: Snapshot ========================

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
                List.of(
                        new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 50, BigDecimal.valueOf(2500000)),
                        new CondicionTicketResponse("CORTESIA", 10, BigDecimal.ZERO),
                        new CondicionTicketResponse("CANCELADO", 5, BigDecimal.valueOf(250000))
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
                List.of(new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 100, BigDecimal.valueOf(5000000))),
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
                List.of(
                        new CondicionTicketResponse("VENDIDO_SIN_ASISTENCIA", 80, BigDecimal.valueOf(4000000)),
                        new CondicionTicketResponse("CORTESIA", 20, BigDecimal.ZERO)
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

    // ======================== US3: Recaudo Incremental ========================

    @Test
    void getRecaudoRetorna200ConRecaudoAcumulado() {
        UUID eventoId = UUID.randomUUID();
        Map<String, BigDecimal> recaudo = Map.of(
                "recaudoRegular", BigDecimal.valueOf(1000000),
                "recaudoCortesia", BigDecimal.ZERO,
                "cancelaciones", BigDecimal.ZERO,
                "recaudoNeto", BigDecimal.valueOf(1000000)
        );
        RecaudoIncrementalResponse response = new RecaudoIncrementalResponse(
                eventoId,
                BigDecimal.valueOf(1000000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000000),
                LocalDateTime.now()
        );

        when(consultarRecaudoIncrementalUseCase.ejecutar(eventoId)).thenReturn(Mono.just(recaudo));
        when(liquidacionRestMapper.toRecaudoResponse(eventoId, recaudo)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/recaudo", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.eventoId").isEqualTo(eventoId.toString())
                .jsonPath("$.recaudoRegular").isEqualTo(1000000)
                .jsonPath("$.recaudoNeto").isEqualTo(1000000);
    }

    @Test
    void getRecaudoNetoDescontaCancelaciones() {
        UUID eventoId = UUID.randomUUID();
        Map<String, BigDecimal> recaudo = Map.of(
                "recaudoRegular", BigDecimal.valueOf(1000000),
                "recaudoCortesia", BigDecimal.ZERO,
                "cancelaciones", BigDecimal.valueOf(200000),
                "recaudoNeto", BigDecimal.valueOf(800000)
        );
        RecaudoIncrementalResponse response = new RecaudoIncrementalResponse(
                eventoId,
                BigDecimal.valueOf(1000000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(200000),
                BigDecimal.valueOf(800000),
                LocalDateTime.now()
        );

        when(consultarRecaudoIncrementalUseCase.ejecutar(eventoId)).thenReturn(Mono.just(recaudo));
        when(liquidacionRestMapper.toRecaudoResponse(eventoId, recaudo)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/recaudo", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.cancelaciones").isEqualTo(200000)
                .jsonPath("$.recaudoNeto").isEqualTo(800000);
    }

    @Test
    void getRecaudoDiferenciaRegularesDeCortesias() {
        UUID eventoId = UUID.randomUUID();
        Map<String, BigDecimal> recaudo = Map.of(
                "recaudoRegular", BigDecimal.valueOf(800000),
                "recaudoCortesia", BigDecimal.valueOf(200000),
                "cancelaciones", BigDecimal.ZERO,
                "recaudoNeto", BigDecimal.valueOf(1000000)
        );
        RecaudoIncrementalResponse response = new RecaudoIncrementalResponse(
                eventoId,
                BigDecimal.valueOf(800000),
                BigDecimal.valueOf(200000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(1000000),
                LocalDateTime.now()
        );

        when(consultarRecaudoIncrementalUseCase.ejecutar(eventoId)).thenReturn(Mono.just(recaudo));
        when(liquidacionRestMapper.toRecaudoResponse(eventoId, recaudo)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{id}/recaudo", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.recaudoRegular").isEqualTo(800000)
                .jsonPath("$.recaudoCortesia").isEqualTo(200000);
    }
}
