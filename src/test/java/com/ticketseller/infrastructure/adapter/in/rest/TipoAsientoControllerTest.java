package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.tipoasiento.AsignarTipoAsientoAZonaUseCase;
import com.ticketseller.application.tipoasiento.CrearTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.DesactivarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.EditarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.ListarTiposAsientoUseCase;
import com.ticketseller.domain.model.asiento.EstadoTipoAsiento;
import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.CrearTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.TipoAsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TipoAsientoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.TipoAsientoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import java.util.UUID;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = TipoAsientoController.class)
@Import(GlobalExceptionHandler.class)
class TipoAsientoControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockBean
    private CrearTipoAsientoUseCase crearTipoAsientoUseCase;
    @MockBean
    private ListarTiposAsientoUseCase listarTiposAsientoUseCase;
    @MockBean
    private EditarTipoAsientoUseCase editarTipoAsientoUseCase;
    @MockBean
    private DesactivarTipoAsientoUseCase desactivarTipoAsientoUseCase;
    @MockBean
    private AsignarTipoAsientoAZonaUseCase asignarTipoAsientoAZonaUseCase;
    @MockBean
    private TipoAsientoRestMapper tipoAsientoRestMapper;
    @MockBean
    private ZonaRestMapper zonaRestMapper;

    @Test
    void crearTipoAsiento_ok() {
        UUID tipoId = UUID.randomUUID();
        var request = new CrearTipoAsientoRequest("VIP", "desc");
        TipoAsiento tipo = TipoAsiento.builder()
                .id(tipoId)
                .nombre("VIP")
                .descripcion("desc")
                .estado(EstadoTipoAsiento.ACTIVO)
                .build();
        Tuple2<TipoAsiento, String> tuple = Tuples.of(tipo, "");
        var response = new TipoAsientoResponse(tipoId, "VIP", "desc", "ACTIVO", false, null);
        when(crearTipoAsientoUseCase.ejecutar("VIP", "desc")).thenReturn(Mono.just(tuple));
        when(tipoAsientoRestMapper.toResponse(tipo, false, null)).thenReturn(response);
        webTestClient.post()
                .uri("/api/v1/tipos-asiento")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(tipoId.toString())
                .jsonPath("$.nombre").isEqualTo("VIP")
                .jsonPath("$.descripcion").isEqualTo("desc")
                .jsonPath("$.estado").isEqualTo("ACTIVO")
                .jsonPath("$.enUso").isEqualTo(false)
                .jsonPath("$.advertencia").doesNotExist();
    }

    @Test
    void listarTiposAsiento_ok() {
        UUID tipoId = UUID.randomUUID();
        TipoAsiento tipo = TipoAsiento.builder()
                .id(tipoId)
                .nombre("VIP")
                .descripcion("desc")
                .estado(EstadoTipoAsiento.ACTIVO)
                .build();
        var response = new TipoAsientoResponse(tipoId, "VIP", "desc", "ACTIVO", false, null);
        when(listarTiposAsientoUseCase.ejecutar(null)).thenReturn(Flux.just(tipo));
        when(listarTiposAsientoUseCase.calcularEnUso(tipo)).thenReturn(Flux.just(false));
        when(tipoAsientoRestMapper.toResponse(tipo, false, null)).thenReturn(response);
        webTestClient.get()
                .uri("/api/v1/tipos-asiento")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(tipoId.toString())
                .jsonPath("$[0].nombre").isEqualTo("VIP")
                .jsonPath("$[0].descripcion").isEqualTo("desc")
                .jsonPath("$[0].estado").isEqualTo("ACTIVO")
                .jsonPath("$[0].enUso").isEqualTo(false)
                .jsonPath("$[0].advertencia").doesNotExist();
    }
}
