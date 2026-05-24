package com.ticketseller.infrastructure.adapter.in.rest.promocion;

import com.ticketseller.application.promocion.AplicarDescuentoCarritoUseCase;
import com.ticketseller.application.promocion.CrearCodigosPromocionalesUseCase;
import com.ticketseller.application.promocion.CrearPromocionUseCase;
import com.ticketseller.application.promocion.DescuentoAplicado;
import com.ticketseller.application.promocion.GestionarEstadoPromocionUseCase;
import com.ticketseller.application.promocion.ListarCodigosPromocionalesUseCase;
import com.ticketseller.application.promocion.ListarPromocionesUseCase;
import com.ticketseller.domain.exception.promocion.TransicionPromocionInvalidaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.MecanismoAplicacion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.PromocionResponse;
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
import java.util.List;
import java.util.UUID;

import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearPromocionRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PromocionController.class)
@Import(GlobalExceptionHandler.class)
class PromocionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearPromocionUseCase crearPromocionUseCase;

    @MockBean
    private GestionarEstadoPromocionUseCase gestionarEstadoPromocionUseCase;

    @MockBean
    private CrearCodigosPromocionalesUseCase crearCodigosPromocionalesUseCase;

    @MockBean
    private AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase;

    @MockBean
    private ListarPromocionesUseCase listarPromocionesUseCase;

    @MockBean
    private ListarCodigosPromocionalesUseCase listarCodigosPromocionalesUseCase;

    @MockBean
    private PromocionRestMapper mapper;

    @Test
    void crearPromocionAutomaticaRetorna201ConPromocionActiva() {
        Promocion promocion = buildPromocionActiva(MecanismoAplicacion.AUTOMATICO);
        PromocionResponse response = buildPromocionResponse(promocion);

        when(mapper.toDomain(any(CrearPromocionRequest.class))).thenReturn(promocion);
        when(crearPromocionUseCase.ejecutar(any())).thenReturn(Mono.just(promocion));
        when(mapper.toResponse(promocion)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/admin/promociones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "nombre": "Descuento VIP",
                          "mecanismo": "AUTOMATICO",
                          "eventoId": "00000000-0000-0000-0000-000000000001",
                          "fechaInicio": "2026-05-10T10:00:00",
                          "fechaFin": "2026-05-20T10:00:00",
                          "tipoUsuarioRestringido": "VIP"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("ACTIVA");
    }

    @Test
    void calcularDescuentosConUsuarioVipAplicaDescuentosCorrespondientes() {
        DescuentoAplicado descuentoAplicado = new DescuentoAplicado(
                new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"));
        com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse responseDto =
                new com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse(
                        new BigDecimal("100000"), new BigDecimal("10000"), new BigDecimal("90000"));

        when(mapper.toItems(any())).thenReturn(List.of());
        when(aplicarDescuentoCarritoUseCase.ejecutar(any(), eq(TipoUsuario.VIP), any()))
                .thenReturn(Mono.just(descuentoAplicado));
        when(mapper.toResponse(descuentoAplicado)).thenReturn(responseDto);

        webTestClient.post()
                .uri("/api/v1/admin/promociones/calcular-descuentos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventoId": "00000000-0000-0000-0000-000000000001",
                          "tipoUsuario": "VIP",
                          "items": [{"zonaId": "00000000-0000-0000-0000-000000000002", "precio": 100000}]
                        }
                        """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void calcularDescuentosConUsuarioGeneralSoloAplicaDescuentosSinRestriccion() {
        DescuentoAplicado sinDescuento = new DescuentoAplicado(
                new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));
        com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse responseDto =
                new com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse(
                        new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("100000"));

        when(mapper.toItems(any())).thenReturn(List.of());
        when(aplicarDescuentoCarritoUseCase.ejecutar(any(), eq(TipoUsuario.GENERAL), any()))
                .thenReturn(Mono.just(sinDescuento));
        when(mapper.toResponse(sinDescuento)).thenReturn(responseDto);

        webTestClient.post()
                .uri("/api/v1/admin/promociones/calcular-descuentos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventoId": "00000000-0000-0000-0000-000000000001",
                          "tipoUsuario": "GENERAL",
                          "items": [{"zonaId": "00000000-0000-0000-0000-000000000002", "precio": 100000}]
                        }
                        """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actualizarEstadoConPausadaRetorna200() {
        UUID id = UUID.randomUUID();
        Promocion pausada = buildPromocionActiva(MecanismoAplicacion.AUTOMATICO).toBuilder()
                .estado(EstadoPromocion.PAUSADA)
                .build();
        PromocionResponse response = buildPromocionResponse(pausada);

        when(gestionarEstadoPromocionUseCase.ejecutar(eq(id), eq(EstadoPromocion.PAUSADA)))
                .thenReturn(Mono.just(pausada));
        when(mapper.toResponse(pausada)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/admin/promociones/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"PAUSADA\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void descuentoNoCuentaParaCampanaPausada() {
        UUID id = UUID.randomUUID();
        when(gestionarEstadoPromocionUseCase.ejecutar(eq(id), eq(EstadoPromocion.PAUSADA)))
                .thenReturn(Mono.error(new TransicionPromocionInvalidaException("Transición inválida")));

        webTestClient.patch()
                .uri("/api/v1/admin/promociones/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"PAUSADA\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void actualizarEstadoConActivaReanudaCampana() {
        UUID id = UUID.randomUUID();
        Promocion activa = buildPromocionActiva(MecanismoAplicacion.CODIGO);
        PromocionResponse response = buildPromocionResponse(activa);

        when(gestionarEstadoPromocionUseCase.ejecutar(eq(id), eq(EstadoPromocion.ACTIVA)))
                .thenReturn(Mono.just(activa));
        when(mapper.toResponse(activa)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/admin/promociones/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"ACTIVA\"}")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void finalizarCampanaEsIrreversible() {
        UUID id = UUID.randomUUID();
        when(gestionarEstadoPromocionUseCase.ejecutar(eq(id), eq(EstadoPromocion.ACTIVA)))
                .thenReturn(Mono.error(new TransicionPromocionInvalidaException(
                        "Una promoción finalizada no puede cambiar de estado")));

        webTestClient.patch()
                .uri("/api/v1/admin/promociones/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"ACTIVA\"}")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void crearCodigosRetornaListaDeCodigos() {
        UUID id = UUID.randomUUID();
        CodigoPromocional codigo1 = CodigoPromocional.builder()
                .id(UUID.randomUUID()).codigo("INFLUENCER-AB12CD34").build();
        CodigoPromocional codigo2 = CodigoPromocional.builder()
                .id(UUID.randomUUID()).codigo("INFLUENCER-EF56GH78").build();

        when(crearCodigosPromocionalesUseCase.ejecutar(any(), anyInt(), any(), any(), any()))
                .thenReturn(Flux.just(codigo1, codigo2));

        webTestClient.post()
                .uri("/api/v1/admin/promociones/{id}/codigos", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "cantidad": 100,
                          "usosMaximosPorCodigo": 1,
                          "prefijo": "INFLUENCER",
                          "fechaFin": "2026-12-31T23:59:59"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    private Promocion buildPromocionActiva(MecanismoAplicacion mecanismo) {
        return Promocion.builder()
                .id(UUID.randomUUID())
                .nombre("Campaña Test")
                .mecanismo(mecanismo)
                .eventoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(7))
                .estado(EstadoPromocion.ACTIVA)
                .build();
    }

    private PromocionResponse buildPromocionResponse(Promocion p) {
        return new PromocionResponse(
                p.getId(), p.getNombre(), p.getMecanismo(), p.getEventoId(),
                p.getFechaInicio(), p.getFechaFin(), p.getEstado(), p.getTipoUsuarioRestringido());
    }
}
