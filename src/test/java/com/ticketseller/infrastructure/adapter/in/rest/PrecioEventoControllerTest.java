package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.application.precios.ListarPreciosUseCase;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.ConfigurarPreciosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.PrecioZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.PrecioZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PrecioEventoRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PrecioEventoController.class)
@Import(GlobalExceptionHandler.class)
class PrecioEventoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConfigurarPreciosUseCase configurarPreciosUseCase;

    @MockBean
    private ListarPreciosUseCase listarPreciosUseCase;

    @MockBean
    private PrecioEventoRestMapper precioEventoRestMapper;

    @Test
    void postPreciosValidosRetorna200() {
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        ConfigurarPreciosRequest request = new ConfigurarPreciosRequest(
                List.of(new PrecioZonaRequest(zonaId, BigDecimal.TEN))
        );

        PrecioZona domain = PrecioZona.builder().zonaId(zonaId).precio(BigDecimal.TEN).build();
        PrecioZona saved = PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(zonaId).precio(BigDecimal.TEN).build();
        PrecioZonaResponse response = new PrecioZonaResponse(saved.getId(), eventoId, zonaId, BigDecimal.TEN);

        when(precioEventoRestMapper.toDomain(any(PrecioZonaRequest.class))).thenReturn(domain);
        when(configurarPreciosUseCase.ejecutar(any(), any())).thenReturn(Flux.just(saved));
        when(precioEventoRestMapper.toResponse(saved)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/eventos/{eventoId}/precios", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].zonaId").isEqualTo(zonaId.toString());
    }

    @Test
    void postPreciosConZonaSinPrecioRetorna400() {
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        ConfigurarPreciosRequest request = new ConfigurarPreciosRequest(
                List.of(new PrecioZonaRequest(zonaId, BigDecimal.TEN))
        );

        when(precioEventoRestMapper.toDomain(any(PrecioZonaRequest.class))).thenReturn(PrecioZona.builder().build());
        when(configurarPreciosUseCase.ejecutar(any(), any()))
                .thenReturn(Flux.error(new ZonaSinPrecioException("No se pueden dejar zonas sin precio")));

        webTestClient.post()
                .uri("/api/v1/eventos/{eventoId}/precios", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void getPreciosRetornaListado() {
        UUID eventoId = UUID.randomUUID();
        PrecioZona precio = PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(UUID.randomUUID()).precio(BigDecimal.TEN).build();
        PrecioZonaResponse response = new PrecioZonaResponse(precio.getId(), precio.getEventoId(), precio.getZonaId(), precio.getPrecio());

        when(listarPreciosUseCase.ejecutar(eventoId)).thenReturn(Flux.just(precio));
        when(precioEventoRestMapper.toResponse(precio)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos/{eventoId}/precios", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].eventoId").isEqualTo(eventoId.toString());
    }
}

