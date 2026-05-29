package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.asiento.AsignarAsientosAZonaUseCase;
import com.ticketseller.application.asiento.ConsultarMapaAsientosUseCase;
import com.ticketseller.application.asiento.CrearMapaAsientosUseCase;
import com.ticketseller.application.asiento.MarcarEspacioVacioUseCase;
import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.shared.Pagina;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.MapaAsientosController;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AsientoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.AsientoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ticketseller.infrastructure.config.TestWebSecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MapaAsientosController.class)
@Import({GlobalExceptionHandler.class, TestWebSecurityConfig.class})
class MapaAsientosControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearMapaAsientosUseCase crearMapaAsientosUseCase;

    @MockBean
    private MarcarEspacioVacioUseCase marcarEspacioVacioUseCase;

    @MockBean
    private AsignarAsientosAZonaUseCase asignarAsientosAZonaUseCase;

    @MockBean
    private ConsultarMapaAsientosUseCase consultarMapaAsientosUseCase;

    @MockBean
    private AsientoRestMapper asientoRestMapper;

    @Test
    void asignarAsientosAZonaRetorna200() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .fila("A")
                .columna(1)
                .numero("A1")
                .zonaId(zonaId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build();

        AsientoResponse response = new AsientoResponse(asientoId, "A", 1, "A1", zonaId, EstadoAsiento.DISPONIBLE.name());

        when(asignarAsientosAZonaUseCase.ejecutar(any(), any())).thenReturn(Flux.just(asiento));
        when(asientoRestMapper.toAsientoResponse(asiento)).thenReturn(response);

        String body = """
                {"asientoIds": ["%s"]}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas/{zonaId}/asientos", recintoId, zonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(asientoId.toString())
                .jsonPath("$[0].fila").isEqualTo("A")
                .jsonPath("$[0].estado").isEqualTo("DISPONIBLE");
    }

    @Test
    void asignarAsientosConListaVaciaRetorna400() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas/{zonaId}/asientos", recintoId, zonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"asientoIds\": []}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void asignarAsientosZonaNoEncontradaRetorna404() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        when(asignarAsientosAZonaUseCase.ejecutar(any(), any()))
                .thenReturn(Flux.error(new ZonaNotFoundException("Zona no encontrada: " + zonaId)));

        String body = """
                {"asientoIds": ["%s"]}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas/{zonaId}/asientos", recintoId, zonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void asignarAsientosAsientoNoEncontradoRetorna404() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        when(asignarAsientosAZonaUseCase.ejecutar(any(), any()))
                .thenReturn(Flux.error(new AsientoNotFoundException("Asiento no encontrado: " + asientoId)));

        String body = """
                {"asientoIds": ["%s"]}
                """.formatted(asientoId);

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas/{zonaId}/asientos", recintoId, zonaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void consultarMapaRetorna200ConPaginacion() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        Asiento asiento = Asiento.builder()
                .id(UUID.randomUUID())
                .fila("A")
                .columna(1)
                .numero("A1")
                .zonaId(zonaId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build();

        Pagina<Asiento> pagina = new Pagina<>(List.of(asiento), 1L, 0, 20);
        AsientoResponse response = new AsientoResponse(asiento.getId(), "A", 1, "A1", zonaId, EstadoAsiento.DISPONIBLE.name());

        when(consultarMapaAsientosUseCase.ejecutar(any(), anyInt(), anyInt())).thenReturn(Mono.just(pagina));
        when(asientoRestMapper.toAsientoResponse(asiento)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos/{recintoId}/mapa/asientos?pagina=0&size=20", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content[0].fila").isEqualTo("A")
                .jsonPath("$.totalElements").isEqualTo(1)
                .jsonPath("$.totalPages").isEqualTo(1);
    }

    @Test
    void consultarMapaPaginaDefecto() {
        UUID recintoId = UUID.randomUUID();

        Pagina<Asiento> paginaVacia = new Pagina<>(List.of(), 0L, 0, 20);

        when(consultarMapaAsientosUseCase.ejecutar(any(), anyInt(), anyInt())).thenReturn(Mono.just(paginaVacia));

        webTestClient.get()
                .uri("/api/v1/recintos/{recintoId}/mapa/asientos", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.totalElements").isEqualTo(0);
    }
}
