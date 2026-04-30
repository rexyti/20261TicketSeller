package com.ticketseller.infrastructure.adapter.in.rest.conciliacion;

import com.ticketseller.application.conciliacion.ConfirmarTransaccionUseCase;
import com.ticketseller.application.conciliacion.ListarDiscrepanciaUseCase;
import com.ticketseller.application.conciliacion.ResolverDiscrepanciaUseCase;
import com.ticketseller.application.conciliacion.VerificarPagoUseCase;
import com.ticketseller.domain.exception.conciliacion.PagoEnDiscrepanciaException;
import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.ConfirmarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.ResolverDiscrepanciaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.VerificarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ConciliacionRestMapperImpl;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ConciliacionController.class)
@Import({GlobalExceptionHandler.class, ConciliacionRestMapperImpl.class})
class ConciliacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VerificarPagoUseCase verificarPagoUseCase;

    @MockBean
    private ConfirmarTransaccionUseCase confirmarTransaccionUseCase;

    @MockBean
    private ResolverDiscrepanciaUseCase resolverDiscrepanciaUseCase;

    @MockBean
    private ListarDiscrepanciaUseCase listarDiscrepanciaUseCase;

    @Test
    void verificarPagoMontoCoincideRetorna201() {
        VerificarPagoRequest request = new VerificarPagoRequest(UUID.randomUUID(), BigDecimal.valueOf(100), "ext-001");
        when(verificarPagoUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.just(pago(EstadoConciliacion.VERIFICADO)));

        webTestClient.post()
                .uri("/api/v1/pagos/verificar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("VERIFICADO");
    }

    @Test
    void verificarPagoMontoDiscrepanciaRetorna201() {
        VerificarPagoRequest request = new VerificarPagoRequest(UUID.randomUUID(), BigDecimal.valueOf(50), "ext-002");
        when(verificarPagoUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.just(pago(EstadoConciliacion.EN_DISCREPANCIA)));

        webTestClient.post()
                .uri("/api/v1/pagos/verificar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("EN_DISCREPANCIA");
    }

    @Test
    void confirmarTransaccionIdempotentePorIdExterno() {
        ConfirmarPagoRequest request = new ConfirmarPagoRequest(UUID.randomUUID(), "ext-003", BigDecimal.valueOf(100));
        when(confirmarTransaccionUseCase.ejecutar(any(), any(), any()))
                .thenReturn(Mono.just(pago(EstadoConciliacion.CONFIRMADO)));

        webTestClient.post()
                .uri("/api/v1/pagos/confirmar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("CONFIRMADO");
    }

    @Test
    void listarDiscrepanciasRetornaListado() {
        when(listarDiscrepanciaUseCase.ejecutar())
                .thenReturn(Flux.just(pago(EstadoConciliacion.EN_DISCREPANCIA)));

        webTestClient.get()
                .uri("/api/v1/admin/conciliacion/discrepancias")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].estado").isEqualTo("EN_DISCREPANCIA");
    }

    @Test
    void confirmarManualDiscrepanciaRetorna200() {
        UUID pagoId = UUID.randomUUID();
        ResolverDiscrepanciaRequest request = new ResolverDiscrepanciaRequest(true, "Validado manualmente", UUID.randomUUID());
        when(resolverDiscrepanciaUseCase.ejecutar(any(), anyBoolean(), any(), any()))
                .thenReturn(Mono.just(pago(EstadoConciliacion.CONFIRMADO_MANUALMENTE)));

        webTestClient.patch()
                .uri("/api/v1/admin/conciliacion/discrepancias/{id}/resolver", pagoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("CONFIRMADO_MANUALMENTE");
    }

    @Test
    void rechazarDiscrepanciaRetorna200() {
        UUID pagoId = UUID.randomUUID();
        ResolverDiscrepanciaRequest request = new ResolverDiscrepanciaRequest(false, "Monto incorrecto", UUID.randomUUID());
        when(resolverDiscrepanciaUseCase.ejecutar(any(), anyBoolean(), any(), any()))
                .thenReturn(Mono.just(pago(EstadoConciliacion.EXPIRADO)));

        webTestClient.patch()
                .uri("/api/v1/admin/conciliacion/discrepancias/{id}/resolver", pagoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("EXPIRADO");
    }

    @Test
    void resolverPagoNoEnDiscrepanciaRetornaConflict() {
        UUID pagoId = UUID.randomUUID();
        ResolverDiscrepanciaRequest request = new ResolverDiscrepanciaRequest(true, "Justif", UUID.randomUUID());
        when(resolverDiscrepanciaUseCase.ejecutar(any(), anyBoolean(), any(), any()))
                .thenReturn(Mono.error(new PagoEnDiscrepanciaException(pagoId)));

        webTestClient.patch()
                .uri("/api/v1/admin/conciliacion/discrepancias/{id}/resolver", pagoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private Pago pago(EstadoConciliacion estado) {
        return Pago.builder()
                .id(UUID.randomUUID())
                .ventaId(UUID.randomUUID())
                .idExternoPasarela("ext-001")
                .montoEsperado(BigDecimal.valueOf(100))
                .montoPasarela(BigDecimal.valueOf(100))
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
    }
}
