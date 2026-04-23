package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.evento.CancelarEventoUseCase;
import com.ticketseller.application.evento.EditarEventoUseCase;
import com.ticketseller.application.evento.ListarEventosUseCase;
import com.ticketseller.application.evento.RegistrarEventoUseCase;
import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.exception.EventoSolapamientoException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.CrearEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.EventoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.EventoRestMapper;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = EventoController.class)
@Import(GlobalExceptionHandler.class)
class EventoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RegistrarEventoUseCase registrarEventoUseCase;

    @MockBean
    private ListarEventosUseCase listarEventosUseCase;

    @MockBean
    private EditarEventoUseCase editarEventoUseCase;

    @MockBean
    private CancelarEventoUseCase cancelarEventoUseCase;

    @MockBean
    private EventoRestMapper eventoRestMapper;

    @Test
    void postEventoValidoRetorna201() {
        UUID recintoId = UUID.randomUUID();
        CrearEventoRequest request = new CrearEventoRequest(
                "Concierto A",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "MUSICAL",
                recintoId
        );

        Evento eventoDomain = Evento.builder().nombre("Concierto A").build();
        Evento eventoSaved = Evento.builder()
                .id(UUID.randomUUID())
                .nombre("Concierto A")
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .tipo("MUSICAL")
                .recintoId(recintoId)
                .estado(EstadoEvento.ACTIVO)
                .build();
        EventoResponse response = new EventoResponse(
                eventoSaved.getId(),
                eventoSaved.getNombre(),
                eventoSaved.getFechaInicio(),
                eventoSaved.getFechaFin(),
                eventoSaved.getTipo(),
                eventoSaved.getRecintoId(),
                eventoSaved.getEstado(),
                eventoSaved.getMotivoCancelacion()
        );

        when(eventoRestMapper.toDomain(any(CrearEventoRequest.class))).thenReturn(eventoDomain);
        when(registrarEventoUseCase.ejecutar(eventoDomain)).thenReturn(Mono.just(eventoSaved));
        when(eventoRestMapper.toResponse(eventoSaved)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Concierto A");
    }

    @Test
    void getEventosRetornaListado() {
        Evento evento = Evento.builder().id(UUID.randomUUID()).nombre("Concierto").estado(EstadoEvento.ACTIVO).build();
        EventoResponse response = new EventoResponse(evento.getId(), evento.getNombre(), evento.getFechaInicio(),
                evento.getFechaFin(), evento.getTipo(), evento.getRecintoId(), evento.getEstado(), evento.getMotivoCancelacion());

        when(listarEventosUseCase.ejecutar(null)).thenReturn(Flux.just(evento));
        when(eventoRestMapper.toResponse(evento)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/eventos")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nombre").isEqualTo("Concierto");
    }

    @Test
    void postEventoSolapadoRetorna409() {
        UUID recintoId = UUID.randomUUID();
        CrearEventoRequest request = new CrearEventoRequest(
                "Concierto A",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "MUSICAL",
                recintoId
        );
        Evento eventoDomain = Evento.builder().nombre("Concierto A").build();

        when(eventoRestMapper.toDomain(any(CrearEventoRequest.class))).thenReturn(eventoDomain);
        when(registrarEventoUseCase.ejecutar(eventoDomain))
                .thenReturn(Mono.error(new EventoSolapamientoException("Evento solapado")));

        webTestClient.post()
                .uri("/api/v1/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void patchEventoNoEncontradoRetorna404() {
        UUID eventoId = UUID.randomUUID();
        when(editarEventoUseCase.ejecutar(any(), any())).thenReturn(Mono.error(new EventoNotFoundException("Evento no encontrado")));

        webTestClient.patch()
                .uri("/api/v1/eventos/{id}", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "nombre": "Editado"
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }
}

