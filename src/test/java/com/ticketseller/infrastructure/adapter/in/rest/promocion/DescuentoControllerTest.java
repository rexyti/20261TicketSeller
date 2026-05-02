package com.ticketseller.infrastructure.adapter.in.rest.promocion;

import com.ticketseller.application.promocion.CrearDescuentoUseCase;
import com.ticketseller.application.promocion.ValidarCodigoPromocionalUseCase;
import com.ticketseller.domain.exception.promocion.CodigoPromoAgotadoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoExpiradoException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoResponse;
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

import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearDescuentoRequest;
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
    private ValidarCodigoPromocionalUseCase validarCodigoPromocionalUseCase;

    @MockBean
    private PromocionRestMapper mapper;

    @Test
    void crearDescuentoPorcentualRetorna201() {
        UUID promocionId = UUID.randomUUID();
        Descuento descuento = buildDescuento(TipoDescuento.PORCENTAJE, "20");
        DescuentoResponse response = buildResponse(descuento);

        when(mapper.toDomain(any(CrearDescuentoRequest.class))).thenReturn(descuento);
        when(crearDescuentoUseCase.ejecutar(eq(promocionId), any())).thenReturn(Mono.just(descuento));
        when(mapper.toResponse(descuento)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/admin/promociones/{id}/descuentos", promocionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "tipo": "PORCENTAJE",
                          "valor": 20,
                          "acumulable": false
                        }
                        """)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void crearDescuentoMontoFijoRetorna201() {
        UUID promocionId = UUID.randomUUID();
        Descuento descuento = buildDescuento(TipoDescuento.MONTO_FIJO, "15000");
        DescuentoResponse response = buildResponse(descuento);

        when(mapper.toDomain(any(CrearDescuentoRequest.class))).thenReturn(descuento);
        when(crearDescuentoUseCase.ejecutar(eq(promocionId), any())).thenReturn(Mono.just(descuento));
        when(mapper.toResponse(descuento)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/admin/promociones/{id}/descuentos", promocionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "tipo": "MONTO_FIJO",
                          "valor": 15000,
                          "acumulable": false
                        }
                        """)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void aplicarCodigoValidoRetorna200ConDescuento() {
        Descuento descuento = buildDescuento(TipoDescuento.PORCENTAJE, "10");
        DescuentoResponse response = buildResponse(descuento);

        when(validarCodigoPromocionalUseCase.ejecutar("AMIGO20")).thenReturn(Mono.just(descuento));
        when(mapper.toResponse(descuento)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"AMIGO20\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void aplicarCodigoYaUsadoRetorna409ConMensaje() {
        when(validarCodigoPromocionalUseCase.ejecutar("USED"))
                .thenReturn(Mono.error(new CodigoPromoAgotadoException("CÓDIGO YA UTILIZADO")));

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"USED\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("CÓDIGO YA UTILIZADO");
    }

    @Test
    void aplicarCodigoExpiradoRetorna409ConMensaje() {
        when(validarCodigoPromocionalUseCase.ejecutar("EXPIRED"))
                .thenReturn(Mono.error(new CodigoPromoExpiradoException("CÓDIGO EXPIRADO")));

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"EXPIRED\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("CÓDIGO EXPIRADO");
    }

    @Test
    void aplicarCodigoConLimiteAgotadoRetorna409() {
        when(validarCodigoPromocionalUseCase.ejecutar("LLENO"))
                .thenReturn(Mono.error(new CodigoPromoAgotadoException("CÓDIGO YA UTILIZADO")));

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"LLENO\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void carritoCalculaPrecioConDescuentoAplicado() {
        Descuento descuento = buildDescuento(TipoDescuento.PORCENTAJE, "20");
        DescuentoResponse response = buildResponse(descuento);

        when(validarCodigoPromocionalUseCase.ejecutar("DESCUENTO20")).thenReturn(Mono.just(descuento));
        when(mapper.toResponse(descuento)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"DESCUENTO20\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tipo").isEqualTo("PORCENTAJE");
    }

    @Test
    void carritoMuestraPrecioSinDescuentoFueraDeVigencia() {
        when(validarCodigoPromocionalUseCase.ejecutar("VENCIDO"))
                .thenReturn(Mono.error(new CodigoPromoExpiradoException("CÓDIGO EXPIRADO")));

        webTestClient.post()
                .uri("/api/v1/compras/carrito/aplicar-codigo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\": \"VENCIDO\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private Descuento buildDescuento(TipoDescuento tipo, String valor) {
        return Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(UUID.randomUUID())
                .tipo(tipo)
                .valor(new BigDecimal(valor))
                .acumulable(false)
                .build();
    }

    private DescuentoResponse buildResponse(Descuento d) {
        return new DescuentoResponse(d.getId(), d.getPromocionId(), d.getTipo(), d.getValor(), d.getZonaId(), d.isAcumulable());
    }
}
