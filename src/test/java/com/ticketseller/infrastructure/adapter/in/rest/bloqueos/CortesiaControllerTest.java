package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.CrearCortesiaConAsientoUseCase;
import com.ticketseller.application.bloqueos.CrearCortesiaGeneralUseCase;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.config.TestWebSecurityConfig;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CortesiaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CortesiaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CortesiaController.class)
@Import({GlobalExceptionHandler.class, TestWebSecurityConfig.class})
class CortesiaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearCortesiaConAsientoUseCase crearCortesiaConAsientoUseCase;

    @MockBean
    private CrearCortesiaGeneralUseCase crearCortesiaGeneralUseCase;

    @MockBean
    private CortesiaRestMapper cortesiaRestMapper;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();
    private final UUID cortesiaId = UUID.randomUUID();
    private final UUID destinatarioId = UUID.randomUUID();
    private static final String EMAIL = "patrocinador@vip.com";

    @Test
    void postCortesiaConAsientoRetorna201() {
        Cortesia cortesia = buildCortesia(asientoId);
        CortesiaResponse response = buildResponse(cortesia);

        when(crearCortesiaConAsientoUseCase.ejecutar(eq(eventoId), eq(destinatarioId), eq(EMAIL),
                eq(CategoriaCortesia.PATROCINADOR), eq(asientoId)))
                .thenReturn(Mono.just(cortesia));
        when(cortesiaRestMapper.toCortesiaResponse(cortesia)).thenReturn(response);

        String body = """
                {"destinatarioId":"%s","emailDestinatario":"%s","categoria":"PATROCINADOR","asientoId":"%s"}
                """.formatted(destinatarioId, EMAIL, asientoId);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(UUID.randomUUID().toString()).roles("COORDINADOR_PATROCINIOS"))
                .post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias/con-asiento", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.cortesiaId").isNotEmpty();
    }

    @Test
    void postCortesiaGeneralRetorna201() {
        Cortesia cortesia = buildCortesia(null);
        CortesiaResponse response = buildResponse(cortesia);

        when(crearCortesiaGeneralUseCase.ejecutar(eq(eventoId), eq(destinatarioId), eq("prensa@abc.com"),
                eq(CategoriaCortesia.PRENSA), eq(zonaId)))
                .thenReturn(Mono.just(cortesia));
        when(cortesiaRestMapper.toCortesiaResponse(cortesia)).thenReturn(response);

        String body = """
                {"destinatarioId":"%s","emailDestinatario":"prensa@abc.com","categoria":"PRENSA","zonaId":"%s"}
                """.formatted(destinatarioId, zonaId);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(UUID.randomUUID().toString()).roles("COORDINADOR_PATROCINIOS"))
                .post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias/general", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.cortesiaId").isNotEmpty();
    }

    @Test
    void postCortesiaGeneralSinZonaRetorna201() {
        Cortesia cortesia = buildCortesia(null);
        CortesiaResponse response = buildResponse(cortesia);

        when(crearCortesiaGeneralUseCase.ejecutar(eq(eventoId), eq(destinatarioId), eq("artista@inv.com"),
                eq(CategoriaCortesia.ARTISTA), eq(null)))
                .thenReturn(Mono.just(cortesia));
        when(cortesiaRestMapper.toCortesiaResponse(cortesia)).thenReturn(response);

        String body = """
                {"destinatarioId":"%s","emailDestinatario":"artista@inv.com","categoria":"ARTISTA"}
                """.formatted(destinatarioId);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(UUID.randomUUID().toString()).roles("COORDINADOR_PATROCINIOS"))
                .post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias/general", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.cortesiaId").isNotEmpty();
    }

    @Test
    void postCortesiaConAsientoOcupadoRetorna409() {
        when(crearCortesiaConAsientoUseCase.ejecutar(eq(eventoId), any(), any(), any(), eq(asientoId)))
                .thenReturn(Mono.error(new AsientoOcupadoException(asientoId)));

        String body = """
                {"destinatarioId":"%s","emailDestinatario":"invitado@test.com","categoria":"OTRO","asientoId":"%s"}
                """.formatted(destinatarioId, asientoId);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(UUID.randomUUID().toString()).roles("COORDINADOR_PATROCINIOS"))
                .post()
                .uri("/api/v1/admin/eventos/{eventoId}/cortesias/con-asiento", eventoId)
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
                .destinatarioId(destinatarioId)
                .emailDestinatario(EMAIL)
                .categoria(CategoriaCortesia.PATROCINADOR)
                .estado(EstadoCortesia.GENERADA)
                .build();
    }

    private CortesiaResponse buildResponse(Cortesia cortesia) {
        return new CortesiaResponse(cortesia.getId(),
                cortesia.getDestinatarioId(), cortesia.getEmailDestinatario(),
                "PATROCINADOR", cortesia.getAsientoId(), cortesia.getTicketId());
    }
}
