package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.CrearCortesiaUseCase;
import com.ticketseller.application.GestionarCortesiaUseCase;
import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.model.CategoriaCortesia;
import com.ticketseller.domain.model.Cortesia;
import com.ticketseller.domain.model.EstadoCortesia;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CrearCortesiaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

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
    private ConsultarPanelBloqueosUseCase consultarPanelBloqueosUseCase;

    @MockBean
    private GestionarCortesiaUseCase gestionarCortesiaUseCase;

    @Test
    void crearCortesiaConAsientoRetorna201() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID cortesiaId = UUID.randomUUID();

        CrearCortesiaRequest request = new CrearCortesiaRequest("Invitado VIP", "PATROCINADOR", asientoId, zonaId);
        Cortesia cortesia = Cortesia.builder()
                .id(cortesiaId)
                .eventoId(eventoId)
                .asientoId(asientoId)
                .ticketId(ticketId)
                .destinatario("Invitado VIP")
                .categoria(CategoriaCortesia.PATROCINADOR)
                .codigoUnico("ABC12345")
                .estado(EstadoCortesia.GENERADA)
                .build();

        when(crearCortesiaUseCase.ejecutar(eventoId, "Invitado VIP", CategoriaCortesia.PATROCINADOR, asientoId, zonaId))
                .thenReturn(Mono.just(cortesia));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.cortesiaId").isEqualTo(cortesiaId.toString())
                .jsonPath("$.asientoId").isEqualTo(asientoId.toString())
                .jsonPath("$.codigoUnico").isEqualTo("ABC12345");
    }

    @Test
    void crearCortesiaSinAsientoRetorna201() {
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID cortesiaId = UUID.randomUUID();

        CrearCortesiaRequest request = new CrearCortesiaRequest("Prensa", "PRENSA", null, zonaId);
        Cortesia cortesia = Cortesia.builder()
                .id(cortesiaId)
                .eventoId(eventoId)
                .asientoId(null)
                .ticketId(UUID.randomUUID())
                .destinatario("Prensa")
                .categoria(CategoriaCortesia.PRENSA)
                .codigoUnico("XYZ56789")
                .estado(EstadoCortesia.GENERADA)
                .build();

        when(crearCortesiaUseCase.ejecutar(eventoId, "Prensa", CategoriaCortesia.PRENSA, null, zonaId))
                .thenReturn(Mono.just(cortesia));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.asientoId").isEmpty()
                .jsonPath("$.categoria").isEqualTo("PRENSA");
    }

    @Test
    void crearCortesiaConAsientoOcupadoRetorna409() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        CrearCortesiaRequest request = new CrearCortesiaRequest("Invitado", "ARTISTA", asientoId, zonaId);

        when(crearCortesiaUseCase.ejecutar(eq(eventoId), eq("Invitado"), eq(CategoriaCortesia.ARTISTA), eq(asientoId), eq(zonaId)))
                .thenReturn(Mono.error(new AsientoOcupadoException("Asiento ocupado")));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/cortesias", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void consultarCortesiasRetorna200() {
        UUID eventoId = UUID.randomUUID();
        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .destinatario("Invitado")
                .categoria(CategoriaCortesia.OTRO)
                .codigoUnico("COD1")
                .estado(EstadoCortesia.GENERADA)
                .build();

        when(consultarPanelBloqueosUseCase.consultarCortesias(eventoId)).thenReturn(Flux.just(cortesia));

        webTestClient.get()
                .uri("/api/admin/eventos/{eventoId}/cortesias", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].eventoId").doesNotExist()
                .jsonPath("$[0].destinatario").isEqualTo("Invitado");
    }
}
