package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.promocion.AplicacionDescuentoResultado;
import com.ticketseller.application.promocion.AplicarCodigoPromocionalCarritoUseCase;
import com.ticketseller.application.promocion.CrearDescuentoCommand;
import com.ticketseller.application.promocion.CrearDescuentoUseCase;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.AplicacionDescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.AplicarCodigoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.DescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
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

@WebFluxTest(controllers = DescuentoController.class)
@Import(GlobalExceptionHandler.class)
class DescuentoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearDescuentoUseCase crearDescuentoUseCase;

    @MockBean
    private AplicarCodigoPromocionalCarritoUseCase aplicarCodigoPromocionalCarritoUseCase;

    @MockBean
    private PromocionRestMapper mapper;

    @Test
    void debeCrearDescuentoYRetornar201() {
        UUID promocionId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        CrearDescuentoRequest request = new CrearDescuentoRequest(
                TipoDescuento.PORCENTAJE,
                BigDecimal.valueOf(20),
                zonaId,
                false
        );

        Descuento descuento = Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(promocionId)
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(BigDecimal.valueOf(20))
                .zonaId(zonaId)
                .acumulable(false)
                .build();

        when(crearDescuentoUseCase.ejecutar(any(CrearDescuentoCommand.class))).thenReturn(Mono.just(descuento));
        when(mapper.toCommand(eq(promocionId), any(CrearDescuentoRequest.class))).thenReturn(
                new CrearDescuentoCommand(promocionId, request.tipo(), request.valor(), request.zonaId(), request.acumulable())
        );
        when(mapper.toResponse(any(Descuento.class))).thenReturn(new DescuentoResponse(
                descuento.getId(), promocionId, TipoDescuento.PORCENTAJE, BigDecimal.valueOf(20), zonaId, false
        ));

        webTestClient.post()
                .uri("/api/v1/admin/promociones/{promocionId}/descuentos", promocionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(descuento.getId().toString())
                .jsonPath("$.valor").isEqualTo(20);
    }

    @Test
    void debeAplicarCodigoYRetornar200() {
        UUID ventaId = UUID.randomUUID();
        AplicarCodigoRequest request = new AplicarCodigoRequest(ventaId, "TEST-123");
        AplicacionDescuentoResultado resultado = new AplicacionDescuentoResultado(
                BigDecimal.valueOf(100), BigDecimal.valueOf(20), BigDecimal.valueOf(80), UUID.randomUUID(), "Codigo aplicado");
        when(aplicarCodigoPromocionalCarritoUseCase.ejecutar(ventaId, "TEST-123"))
                .thenReturn(Mono.just(resultado));
        when(mapper.toResponse(any(AplicacionDescuentoResultado.class))).thenReturn(new AplicacionDescuentoResponse(
                resultado.descuentoId(), resultado.subtotal(), resultado.montoDescuento(), resultado.totalFinal(), resultado.descripcion()
        ));

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.montoDescuento").isEqualTo(20)
                .jsonPath("$.totalFinal").isEqualTo(80);
    }
}
