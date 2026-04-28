package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConsultarEstructuraRecintoUseCase;
import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosFiltradosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.domain.exception.recinto.RecintoDuplicadoException;
import com.ticketseller.domain.exception.recinto.RecintoInvalidoException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.shared.Pagina;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.CrearRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.RecintoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.RecintoRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = RecintoController.class)
@Import(GlobalExceptionHandler.class)
class RecintoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RegistrarRecintoUseCase registrarRecintoUseCase;

    @MockBean
    private ListarRecintosFiltradosUseCase listarRecintosFiltradosUseCase;

    @MockBean
    private ConfigurarCapacidadUseCase configurarCapacidadUseCase;

    @MockBean
    private EditarRecintoUseCase editarRecintoUseCase;

    @MockBean
    private DesactivarRecintoUseCase desactivarRecintoUseCase;

    @MockBean
    private ConfigurarCategoriaUseCase configurarCategoriaUseCase;

    @MockBean
    private ConsultarEstructuraRecintoUseCase consultarEstructuraRecintoUseCase;

    @MockBean
    private RecintoRestMapper recintoRestMapper;

    @Test
    void postRecintoValidoRetorna201() {
        CrearRecintoRequest request = new CrearRecintoRequest("Movistar Arena", "Bogota", "Calle 1", 1000,
                "3001234567", 4);
        Recinto recintoDomain = Recinto.builder().nombre("Movistar Arena").ciudad("Bogota").build();
        Recinto recintoSaved = Recinto.builder()
                .id(UUID.randomUUID())
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .fechaCreacion(LocalDateTime.now())
                .compuertasIngreso(4)
                .activo(true)
                .build();
        RecintoResponse response = new RecintoResponse(recintoSaved.getId(), recintoSaved.getNombre(), recintoSaved.getCiudad(),
                recintoSaved.getDireccion(), recintoSaved.getCapacidadMaxima(), recintoSaved.getTelefono(),
                recintoSaved.getFechaCreacion(), recintoSaved.getCompuertasIngreso(), recintoSaved.isActivo(), recintoSaved.getCategoria());

        when(recintoRestMapper.toDomain(any(CrearRecintoRequest.class))).thenReturn(recintoDomain);
        when(registrarRecintoUseCase.ejecutar(recintoDomain)).thenReturn(Mono.just(recintoSaved));
        when(recintoRestMapper.toResponse(recintoSaved)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/recintos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Movistar Arena")
                .jsonPath("$.ciudad").isEqualTo("Bogota");
    }

    @Test
    void postRecintoInvalidoRetorna400() {
        webTestClient.post()
                .uri("/api/v1/recintos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "nombre": "",
                          "ciudad": "",
                          "direccion": "",
                          "capacidadMaxima": 0,
                          "telefono": "",
                          "compuertasIngreso": -1
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getRecintosRetornaListadoPaginado() {
        Recinto recinto = Recinto.builder().id(UUID.randomUUID()).nombre("Movistar Arena").ciudad("Bogota").activo(true).build();
        RecintoResponse response = new RecintoResponse(recinto.getId(), recinto.getNombre(), recinto.getCiudad(),
                recinto.getDireccion(), recinto.getCapacidadMaxima(), recinto.getTelefono(), recinto.getFechaCreacion(),
                recinto.getCompuertasIngreso(), recinto.isActivo(), recinto.getCategoria());

        when(listarRecintosFiltradosUseCase.ejecutar(any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(Mono.just(new Pagina<>(List.of(recinto), 1, 0, 10)));
        when(recintoRestMapper.toResponse(recinto)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/recintos")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void postRecintoDuplicadoRetorna409() {
        CrearRecintoRequest request = new CrearRecintoRequest("Movistar Arena", "Bogota", "Calle 1", 1000,
                "3001234567", 4);
        Recinto recintoDomain = Recinto.builder().nombre("Movistar Arena").ciudad("Bogota").build();

        when(recintoRestMapper.toDomain(any(CrearRecintoRequest.class))).thenReturn(recintoDomain);
        when(registrarRecintoUseCase.ejecutar(recintoDomain))
                .thenReturn(Mono.error(new RecintoDuplicadoException("Ya existe un recinto con el mismo nombre y ciudad")));

        webTestClient.post()
                .uri("/api/v1/recintos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("CONFLICT");
    }

    @Test
    void postRecintoConErrorDeDominioRetorna400() {
        CrearRecintoRequest request = new CrearRecintoRequest("Movistar Arena", "Bogota", "Calle 1", 1000,
                "3001234567", 4);
        Recinto recintoDomain = Recinto.builder().nombre("Movistar Arena").ciudad("Bogota").build();

        when(recintoRestMapper.toDomain(any(CrearRecintoRequest.class))).thenReturn(recintoDomain);
        when(registrarRecintoUseCase.ejecutar(recintoDomain))
                .thenReturn(Mono.error(new RecintoInvalidoException("El campo nombre es obligatorio")));

        webTestClient.post()
                .uri("/api/v1/recintos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void patchCapacidadValidaRetorna200() {
        UUID id = UUID.randomUUID();
        Recinto updated = Recinto.builder()
                .id(id)
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1200)
                .telefono("3001234567")
                .fechaCreacion(LocalDateTime.now())
                .compuertasIngreso(4)
                .activo(true)
                .build();
        RecintoResponse response = new RecintoResponse(updated.getId(), updated.getNombre(), updated.getCiudad(),
                updated.getDireccion(), updated.getCapacidadMaxima(), updated.getTelefono(), updated.getFechaCreacion(),
                updated.getCompuertasIngreso(), updated.isActivo(), updated.getCategoria());

        when(configurarCapacidadUseCase.ejecutar(id, 1200)).thenReturn(Mono.just(updated));
        when(recintoRestMapper.toResponse(updated)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/recintos/{id}/capacidad", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "capacidadMaxima": 1200
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.capacidadMaxima").isEqualTo(1200);
    }

    @Test
    void patchCapacidadInvalidaRetorna400() {
        UUID id = UUID.randomUUID();

        webTestClient.patch()
                .uri("/api/v1/recintos/{id}/capacidad", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "capacidadMaxima": 0
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void patchCapacidadRecintoInexistenteRetorna404() {
        UUID id = UUID.randomUUID();
        when(configurarCapacidadUseCase.ejecutar(id, 1200))
                .thenReturn(Mono.error(new RecintoNotFoundException("Recinto no encontrado")));

        webTestClient.patch()
                .uri("/api/v1/recintos/{id}/capacidad", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "capacidadMaxima": 1200
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }
}







