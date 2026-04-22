package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.CrearZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ZonaController.class)
@Import(GlobalExceptionHandler.class)
class ZonaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearZonaUseCase crearZonaUseCase;

    @MockBean
    private ListarZonasUseCase listarZonasUseCase;

    @MockBean
    private ZonaRestMapper zonaRestMapper;

    @Test
    void postZonaValidaRetorna201() {
        UUID recintoId = UUID.randomUUID();
        CrearZonaRequest request = new CrearZonaRequest("Platea", 100);
        Zona zonaDomain = Zona.builder().nombre("Platea").capacidad(100).build();
        Zona zonaSaved = Zona.builder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .nombre("Platea")
                .capacidad(100)
                .build();
        ZonaResponse response = new ZonaResponse(zonaSaved.getId(), recintoId, "Platea", 100);

        when(zonaRestMapper.toDomain(any(CrearZonaRequest.class))).thenReturn(zonaDomain);
        when(crearZonaUseCase.ejecutar(recintoId, zonaDomain)).thenReturn(Mono.just(zonaSaved));
        when(zonaRestMapper.toResponse(zonaSaved)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas", recintoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Platea")
                .jsonPath("$.capacidad").isEqualTo(100);
    }

    @Test
    void postZonaInvalidaRetorna400() {
        UUID recintoId = UUID.randomUUID();

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas", recintoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "nombre": "",
                          "capacidad": 0
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getZonasRetornaListado() {
        UUID recintoId = UUID.randomUUID();
        Zona zona = Zona.builder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .nombre("VIP")
                .capacidad(50)
                .build();
        ZonaResponse response = new ZonaResponse(zona.getId(), recintoId, "VIP", 50);

        when(listarZonasUseCase.ejecutar(recintoId)).thenReturn(Flux.just(zona));
        when(zonaRestMapper.toResponse(zona)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos/{recintoId}/zonas", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("VIP");
    }

    @Test
    void postZonaExcedidaRetorna400() {
        UUID recintoId = UUID.randomUUID();
        CrearZonaRequest request = new CrearZonaRequest("Platea", 1000);
        Zona zonaDomain = Zona.builder().nombre("Platea").capacidad(1000).build();

        when(zonaRestMapper.toDomain(any(CrearZonaRequest.class))).thenReturn(zonaDomain);
        when(crearZonaUseCase.ejecutar(recintoId, zonaDomain))
                .thenReturn(Mono.error(new ZonaCapacidadExcedidaException("Capacidad excedida")));

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/zonas", recintoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDATION_ERROR");
    }
}

