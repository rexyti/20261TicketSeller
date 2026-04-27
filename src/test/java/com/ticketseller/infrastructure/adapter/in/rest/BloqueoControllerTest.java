package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.BloquearAsientosUseCase;
import com.ticketseller.application.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.GestionarBloqueoUseCase;
import com.ticketseller.domain.exception.AsientoOcupadoException;
import com.ticketseller.domain.exception.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.Bloqueo;
import com.ticketseller.domain.model.EstadoBloqueo;
import com.ticketseller.infrastructure.adapter.in.rest.dto.BloquearAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.EditarBloqueoRequest;
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

    @Test
    void bloquearAsientosDisponiblesRetorna201() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        UUID bloqueoId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        BloquearAsientosRequest request = new BloquearAsientosRequest(List.of(asientoId), "Sponsor A", now.plusDays(1));
        Bloqueo bloqueo = Bloqueo.builder()
                .id(bloqueoId)
                .asientoId(asientoId)
                .eventoId(eventoId)
                .destinatario("Sponsor A")
                .estado(EstadoBloqueo.ACTIVO)
                .fechaCreacion(now)
                .fechaExpiracion(now.plusDays(1))
                .build();

        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), eq(List.of(asientoId)), eq("Sponsor A"), any()))
                .thenReturn(Mono.just(List.of(bloqueo)));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$[0].asientoId").isEqualTo(asientoId.toString())
                .jsonPath("$[0].estado").isEqualTo("ACTIVO");
    }

    @Test
    void bloquearAsientoYaBloqueadoRetorna409() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        BloquearAsientosRequest request = new BloquearAsientosRequest(List.of(asientoId), "Sponsor A", null);

        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), eq(List.of(asientoId)), eq("Sponsor A"), eq(null)))
                .thenReturn(Mono.error(new AsientoYaBloqueadoException("Ya bloqueado")));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void bloquearAsientoOcupadoRetorna409() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        BloquearAsientosRequest request = new BloquearAsientosRequest(List.of(asientoId), "Sponsor A", null);

        when(bloquearAsientosUseCase.ejecutar(eq(eventoId), eq(List.of(asientoId)), eq("Sponsor A"), eq(null)))
                .thenReturn(Mono.error(new AsientoOcupadoException("No disponible")));

        webTestClient.post()
                .uri("/api/admin/eventos/{eventoId}/bloqueos", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void consultarBloqueosRetorna200() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        Bloqueo bloqueo = Bloqueo.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .asientoId(asientoId)
                .destinatario("Sponsor")
                .estado(EstadoBloqueo.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(consultarPanelBloqueosUseCase.consultarBloqueos(eventoId)).thenReturn(Flux.just(bloqueo));

        webTestClient.get()
                .uri("/api/admin/eventos/{eventoId}/bloqueos", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].eventoId").isEqualTo(eventoId.toString());
    }

    @Test
    void editarBloqueoRetorna200() {
        UUID bloqueoId = UUID.randomUUID();
        Bloqueo bloqueo = Bloqueo.builder()
                .id(bloqueoId)
                .eventoId(UUID.randomUUID())
                .asientoId(UUID.randomUUID())
                .destinatario("Nuevo Sponsor")
                .estado(EstadoBloqueo.ACTIVO)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(gestionarBloqueoUseCase.editarDestinatario(bloqueoId, "Nuevo Sponsor"))
                .thenReturn(Mono.just(bloqueo));

        webTestClient.patch()
                .uri("/api/admin/bloqueos/{bloqueoId}", bloqueoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new EditarBloqueoRequest("Nuevo Sponsor"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.destinatario").isEqualTo("Nuevo Sponsor");
    }

    @Test
    void liberarBloqueoRetorna200() {
        UUID bloqueoId = UUID.randomUUID();
        when(gestionarBloqueoUseCase.liberarBloqueo(bloqueoId)).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/api/admin/bloqueos/{bloqueoId}", bloqueoId)
                .exchange()
                .expectStatus().isOk();
    }
}
