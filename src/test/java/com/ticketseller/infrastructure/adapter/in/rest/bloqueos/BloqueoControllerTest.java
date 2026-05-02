package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.BloquearAsientosUseCase;
import com.ticketseller.application.bloqueos.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.bloqueos.GestionarBloqueoUseCase;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.exception.bloqueos.BloqueoNoEncontradoException;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.application.bloqueos.PanelItem;
import com.ticketseller.application.bloqueos.TipoPanelItem;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.BloqueoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.BloqueoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.PanelItemResponse;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = BloqueoController.class)
@Import(GlobalExceptionHandler.class)
class BloqueoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BloquearAsientosUseCase bloquearAsientosUseCase;

    @MockBean
    private GestionarBloqueoUseCase gestionarBloqueoUseCase;

    @MockBean
    private ConsultarPanelBloqueosUseCase consultarPanelBloqueosUseCase;

    @MockBean
    private BloqueoRestMapper bloqueoRestMapper;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID asientoId = UUID.randomUUID();
    private final UUID bloqueoId = UUID.randomUUID();

    @Test
    void postBloqueoAsientosDisponiblesRetorna201() {
        Bloqueo bloqueo = buildBloqueo();
        BloqueoResponse response = new BloqueoResponse(bloqueoId, List.of(asientoId), "Sponsor", "ACTIVO", LocalDateTime.now());

        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), any(), eq("Sponsor"), any()))
                .thenReturn(Mono.just(List.of(bloqueo)));
        when(bloqueoRestMapper.toBloqueoResponseBatch(any())).thenReturn(response);

        String body = """
                {"asientoIds":["%s"],"destinatario":"Sponsor"}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.destinatario").isEqualTo("Sponsor");
    }

    @Test
    void postBloqueoAsientoYaBloqueadoRetorna409() {
        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), any(), any(), any()))
                .thenReturn(Mono.error(new AsientoYaBloqueadoException(asientoId)));

        String body = """
                {"asientoIds":["%s"],"destinatario":"Sponsor"}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void postBloqueoAsientoOcupadoRetorna409() {
        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), any(), any(), any()))
                .thenReturn(Mono.error(new AsientoOcupadoException(asientoId)));

        String body = """
                {"asientoIds":["%s"],"destinatario":"Sponsor"}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void postBloqueoListaMixtaRetorna409() {
        UUID otroAsientoId = UUID.randomUUID();
        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), any(), any(), any()))
                .thenReturn(Mono.error(new AsientoOcupadoException(otroAsientoId)));

        String body = """
                {"asientoIds":["%s","%s"],"destinatario":"Sponsor"}
                """.formatted(asientoId, otroAsientoId);

        webTestClient.post()
                .uri("/api/v1/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getBloqueosPanelRetorna200ConListaActiva() {
        PanelItem panelItem = new PanelItem(bloqueoId, TipoPanelItem.BLOQUEO, asientoId,
                "Sponsor", "ACTIVO", LocalDateTime.now(), null, null, null);
        PanelItemResponse panelResponse = new PanelItemResponse(bloqueoId, "BLOQUEO", asientoId,
                "Sponsor", "ACTIVO", LocalDateTime.now(), null, null, null);

        when(consultarPanelBloqueosUseCase.ejecutar(eq(eventoId), eq(null))).thenReturn(Flux.just(panelItem));
        when(bloqueoRestMapper.toPanelItemResponse(panelItem)).thenReturn(panelResponse);

        webTestClient.get()
                .uri("/api/v1/admin/eventos/{eventoId}/bloqueos", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].tipo").isEqualTo("BLOQUEO")
                .jsonPath("$[0].destinatario").isEqualTo("Sponsor");
    }

    @Test
    void getBloqueosPanelFiltradoPorCortesiaRetornaSoloCortesias() {
        PanelItem cortesiaItem = new PanelItem(UUID.randomUUID(), TipoPanelItem.CORTESIA, null,
                "Prensa", "GENERADA", LocalDateTime.now(), null, "codigo-123", "PRENSA");
        PanelItemResponse cortesiaResponse = new PanelItemResponse(cortesiaItem.id(), "CORTESIA", null,
                "Prensa", "GENERADA", LocalDateTime.now(), null, "codigo-123", "PRENSA");

        when(consultarPanelBloqueosUseCase.ejecutar(eq(eventoId), eq(TipoPanelItem.CORTESIA)))
                .thenReturn(Flux.just(cortesiaItem));
        when(bloqueoRestMapper.toPanelItemResponse(cortesiaItem)).thenReturn(cortesiaResponse);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/eventos/{eventoId}/bloqueos")
                        .queryParam("tipo", "CORTESIA")
                        .build(eventoId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].tipo").isEqualTo("CORTESIA");
    }

    @Test
    void patchBloqueoActualizaDestinatarioSinCambiarAsiento() {
        Bloqueo actualizado = buildBloqueo().toBuilder().destinatario("Nuevo Sponsor").build();
        BloqueoResponse response = new BloqueoResponse(bloqueoId, List.of(asientoId), "Nuevo Sponsor", "ACTIVO", LocalDateTime.now());

        when(gestionarBloqueoUseCase.editarDestinatario(eq(bloqueoId), eq("Nuevo Sponsor")))
                .thenReturn(Mono.just(actualizado));
        when(bloqueoRestMapper.toBloqueoResponse(actualizado)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/admin/bloqueos/{bloqueoId}", bloqueoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"destinatario\":\"Nuevo Sponsor\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.destinatario").isEqualTo("Nuevo Sponsor");
    }

    @Test
    void deleteBloqueoLiberaAsientoADisponible() {
        when(gestionarBloqueoUseCase.liberarBloqueo(eq(bloqueoId))).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/v1/admin/bloqueos/{bloqueoId}", bloqueoId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void deleteBloqueoNoEncontradoRetorna404() {
        when(gestionarBloqueoUseCase.liberarBloqueo(eq(bloqueoId)))
                .thenReturn(Mono.error(new BloqueoNoEncontradoException(bloqueoId)));

        webTestClient.delete()
                .uri("/api/v1/admin/bloqueos/{bloqueoId}", bloqueoId)
                .exchange()
                .expectStatus().isNotFound();
    }

    private Bloqueo buildBloqueo() {
        return Bloqueo.builder()
                .id(bloqueoId)
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario("Sponsor")
                .estado(EstadoBloqueo.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }
}
