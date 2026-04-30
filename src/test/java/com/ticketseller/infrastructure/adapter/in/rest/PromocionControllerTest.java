package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.promocion.CrearCodigosPromocionalesCommand;
import com.ticketseller.application.promocion.CrearCodigosPromocionalesUseCase;
import com.ticketseller.application.promocion.CrearPromocionCommand;
import com.ticketseller.application.promocion.CrearPromocionUseCase;
import com.ticketseller.application.promocion.GestionarEstadoPromocionUseCase;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CambiarEstadoPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearCodigosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.PromocionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
    private CrearCodigosPromocionalesUseCase crearCodigosPromocionalesUseCase;

    @MockBean
    private GestionarEstadoPromocionUseCase gestionarEstadoPromocionUseCase;

    @MockBean
    private PromocionRestMapper mapper;

    @Test
    void debeCrearPromocionYRetornar201() {
        UUID promocionId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = LocalDateTime.now().plusDays(5);

        CrearPromocionRequest request = new CrearPromocionRequest(
                "Preventa VIP",
                TipoPromocion.PREVENTA,
                eventoId,
                inicio,
                fin,
                TipoUsuario.VIP
        );

        Promocion promocion = Promocion.builder()
                .id(promocionId)
                .nombre("Preventa VIP")
                .tipo(TipoPromocion.PREVENTA)
                .eventoId(eventoId)
                .fechaInicio(inicio)
                .fechaFin(fin)
                .estado(EstadoPromocion.ACTIVA)
                .tipoUsuarioRestringido(TipoUsuario.VIP)
                .build();

        when(crearPromocionUseCase.ejecutar(any(CrearPromocionCommand.class))).thenReturn(Mono.just(promocion));
        when(mapper.toCommand(any(CrearPromocionRequest.class))).thenReturn(
                new CrearPromocionCommand("Preventa VIP", TipoPromocion.PREVENTA, eventoId, inicio, fin, TipoUsuario.VIP)
        );
        when(mapper.toResponse(any(Promocion.class))).thenReturn(new PromocionResponse(
                promocionId, "Preventa VIP", TipoPromocion.PREVENTA, eventoId, inicio, fin, EstadoPromocion.ACTIVA, TipoUsuario.VIP
        ));

        webTestClient.post()
                .uri("/api/v1/admin/promociones")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(promocionId.toString())
                .jsonPath("$.nombre").isEqualTo("Preventa VIP")
                .jsonPath("$.estado").isEqualTo("ACTIVA");
    }

    @Test
    void debeCrearCodigosYRetornar201() {
        UUID promocionId = UUID.randomUUID();
        CrearCodigosRequest request = new CrearCodigosRequest(
                2, 1, "TEST", LocalDateTime.now().plusDays(1)
        );

        CodigoPromocional c1 = CodigoPromocional.builder().id(UUID.randomUUID()).codigo("TEST-1").estado(EstadoCodigoPromocional.ACTIVO).build();
        CodigoPromocional c2 = CodigoPromocional.builder().id(UUID.randomUUID()).codigo("TEST-2").estado(EstadoCodigoPromocional.ACTIVO).build();

        when(crearCodigosPromocionalesUseCase.ejecutar(any(CrearCodigosPromocionalesCommand.class)))
                .thenReturn(Flux.just(c1, c2));

        when(mapper.toCommand(eq(promocionId), any(CrearCodigosRequest.class))).thenReturn(
                new CrearCodigosPromocionalesCommand(promocionId, 2, 1, "TEST", request.fechaFin())
        );
        when(mapper.toResponse(any(CodigoPromocional.class))).thenAnswer(invocation -> {
            CodigoPromocional c = invocation.getArgument(0);
            return new com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CodigoPromocionalResponse(
                    c.getId(), c.getCodigo(), promocionId, c.getUsosMaximos(), c.getUsosActuales(),
                    c.getFechaInicio(), c.getFechaFin(), c.getEstado()
            );
        });

        webTestClient.post()
                .uri("/api/v1/admin/promociones/{promocionId}/codigos", promocionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(Object.class).hasSize(2);
    }

    @Test
    void debeActualizarEstadoYRetornar200() {
        UUID promocionId = UUID.randomUUID();
        CambiarEstadoPromocionRequest request = new CambiarEstadoPromocionRequest(EstadoPromocion.PAUSADA);

        Promocion promocion = Promocion.builder()
                .id(promocionId)
                .nombre("Promo")
                .estado(EstadoPromocion.PAUSADA)
                .build();

        when(gestionarEstadoPromocionUseCase.ejecutar(eq(promocionId), eq(EstadoPromocion.PAUSADA)))
                .thenReturn(Mono.just(promocion));
        when(mapper.toResponse(any(Promocion.class))).thenReturn(new PromocionResponse(
                promocionId, "Promo", TipoPromocion.PREVENTA, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), EstadoPromocion.PAUSADA, null
        ));

        webTestClient.patch()
                .uri("/api/v1/admin/promociones/{promocionId}/estado", promocionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("PAUSADA");
    }
}
