package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.CrearCortesiaUseCase;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CortesiaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CortesiaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CortesiaController.class)
@Import(GlobalExceptionHandler.class)
class CortesiaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearCortesiaUseCase crearCortesiaUseCase;

    @MockBean
    private CortesiaRestMapper cortesiaRestMapper;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID cortesiaId = UUID.randomUUID();

    @Test
    void postCortesiaConAsientoRetorna201ConCodigoUnico() {
        Cortesia cortesia = buildCortesia(asientoId);
        CortesiaResponse response = buildResponse(cortesia);

        when(crearCortesiaUseCase.ejecutar(eq(eventoId), eq("Patrocinador VIP"),
                eq(CategoriaCortesia.PATROCINADOR), eq(asientoId)))
                .thenReturn(Mono.just(cortesia));
        when(cortesiaRestMapper.toCortesiaResponse(cortesia)).thenReturn(response);

        String body = """
                {"destinatario":"Patrocinador VIP","categoria":"PATROCINADOR","asientoId":"%s"}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.codigoUnico").isNotEmpty()
                .jsonPath("$.cortesiaId").isNotEmpty();
    }

    @Test
    void postCortesiaSinAsientoRetorna201ConAccesoGeneral() {
        Cortesia cortesia = buildCortesia(null);
        CortesiaResponse response = buildResponse(cortesia);

        when(crearCortesiaUseCase.ejecutar(eq(eventoId), eq("Prensa ABC"),
                eq(CategoriaCortesia.PRENSA), eq(null)))
                .thenReturn(Mono.just(cortesia));
        when(cortesiaRestMapper.toCortesiaResponse(cortesia)).thenReturn(response);

        String body = """
                {"destinatario":"Prensa ABC","categoria":"PRENSA"}
                """;

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.codigoUnico").isNotEmpty();
    }

    @Test
    void postCortesiaAsientoOcupadoRetorna409() {
        when(crearCortesiaUseCase.ejecutar(eq(eventoId), any(), any(), eq(asientoId)))
                .thenReturn(Mono.error(new AsientoOcupadoException(asientoId)));

        String body = """
                {"destinatario":"Invitado","categoria":"OTRO","asientoId":"%s"}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    private Cortesia buildCortesia(UUID asientoIdRef) {
        return Cortesia.builder()
                .id(cortesiaId)
                .eventoId(eventoId)
                .asientoId(asientoIdRef)
                .destinatario("Invitado")
                .categoria(CategoriaCortesia.PATROCINADOR)
                .codigoUnico(UUID.randomUUID().toString())
                .estado(EstadoCortesia.GENERADA)
                .build();
    }

    private CortesiaResponse buildResponse(Cortesia cortesia) {
        return new CortesiaResponse(cortesia.getId(), cortesia.getCodigoUnico(),
                cortesia.getDestinatario(), "PATROCINADOR", cortesia.getAsientoId(), cortesia.getTicketId());
    }
}
