package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CompuertaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CrearCompuertaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CompuertaRestMapper;
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

@WebFluxTest(controllers = CompuertaController.class)
@Import(GlobalExceptionHandler.class)
class CompuertaControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CrearCompuertaUseCase crearCompuertaUseCase;

    @MockBean
    private AsignarCompuertaAZonaUseCase asignarCompuertaAZonaUseCase;

    @MockBean
    private ListarCompuertasUseCase listarCompuertasUseCase;

    @MockBean
    private CompuertaRestMapper compuertaRestMapper;

    @Test
    void postCompuertaValidaRetorna201() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        CrearCompuertaRequest request = new CrearCompuertaRequest("Puerta Norte", zonaId);
        Compuerta compuertaDomain = Compuerta.builder().nombre("Puerta Norte").zonaId(zonaId).build();
        Compuerta compuertaSaved = Compuerta.builder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .zonaId(zonaId)
                .nombre("Puerta Norte")
                .esGeneral(false)
                .build();
        CompuertaResponse response = new CompuertaResponse(
                compuertaSaved.getId(), recintoId, zonaId, "Puerta Norte", false);

        when(compuertaRestMapper.toDomain(any(CrearCompuertaRequest.class))).thenReturn(compuertaDomain);
        when(crearCompuertaUseCase.ejecutar(recintoId, compuertaDomain)).thenReturn(Mono.just(compuertaSaved));
        when(compuertaRestMapper.toResponse(compuertaSaved)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/compuertas", recintoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Puerta Norte");
    }

    @Test
    void postCompuertaInvalidaRetorna400() {
        UUID recintoId = UUID.randomUUID();

        webTestClient.post()
                .uri("/api/v1/recintos/{recintoId}/compuertas", recintoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "nombre": "",
                          "zonaId": null
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getCompuertasRetornaListado() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        Compuerta compuerta = Compuerta.builder()
                .id(UUID.randomUUID())
                .recintoId(recintoId)
                .zonaId(zonaId)
                .nombre("Puerta Sur")
                .esGeneral(true)
                .build();
        CompuertaResponse response = new CompuertaResponse(compuerta.getId(), recintoId, zonaId, "Puerta Sur", true);

        when(listarCompuertasUseCase.ejecutar(recintoId)).thenReturn(Flux.just(compuerta));
        when(compuertaRestMapper.toResponse(compuerta)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos/{recintoId}/compuertas", recintoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("Puerta Sur");
    }

    @Test
    void patchAsignarCompuertaZonaRetorna200() {
        UUID compuertaId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        Compuerta compuerta = Compuerta.builder()
                .id(compuertaId)
                .recintoId(UUID.randomUUID())
                .zonaId(zonaId)
                .nombre("Puerta Este")
                .esGeneral(false)
                .build();
        CompuertaResponse response = new CompuertaResponse(
                compuerta.getId(), compuerta.getRecintoId(), zonaId, "Puerta Este", false);

        when(asignarCompuertaAZonaUseCase.ejecutar(compuertaId, zonaId)).thenReturn(Mono.just(compuerta));
        when(compuertaRestMapper.toResponse(compuerta)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/recintos/zonas/{zonaId}/compuertas/{compuertaId}", zonaId, compuertaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.zonaId").isEqualTo(zonaId.toString());
    }

    @Test
    void patchAsignarCompuertaZonaRecintoInexistenteRetorna404() {
        UUID compuertaId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(asignarCompuertaAZonaUseCase.ejecutar(compuertaId, zonaId))
                .thenReturn(Mono.error(new RecintoNotFoundException("Compuerta no encontrada")));

        webTestClient.patch()
                .uri("/api/v1/recintos/zonas/{zonaId}/compuertas/{compuertaId}", zonaId, compuertaId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("NOT_FOUND");
    }
}

