package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.evento.CancelarEventoUseCase;
import com.ticketseller.application.evento.EditarEventoUseCase;
import com.ticketseller.application.evento.ListarEventosUseCase;
import com.ticketseller.application.evento.RegistrarEventoUseCase;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.CancelarEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.CrearEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.EditarEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.EventoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.EventoRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final RegistrarEventoUseCase registrarEventoUseCase;
    private final ListarEventosUseCase listarEventosUseCase;
    private final EditarEventoUseCase editarEventoUseCase;
    private final CancelarEventoUseCase cancelarEventoUseCase;
    private final EventoRestMapper eventoRestMapper;

    @PostMapping
    public Mono<ResponseEntity<EventoResponse>> crear(@Valid @RequestBody CrearEventoRequest request) {
        return registrarEventoUseCase.ejecutar(eventoRestMapper.toDomain(request))
                .map(eventoRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping
    public Flux<EventoResponse> listar(@RequestParam(required = false) String estado) {
        EstadoEvento filtroEstado = estado == null || estado.isBlank() ? null : EstadoEvento.valueOf(estado.toUpperCase());
        return listarEventosUseCase.ejecutar(filtroEstado).map(eventoRestMapper::toResponse);
    }

    @PatchMapping("/{id}")
    public Mono<ResponseEntity<EventoResponse>> editar(@PathVariable UUID id,
                                                       @RequestBody EditarEventoRequest request) {
        Evento cambios = eventoRestMapper.toDomain(request);
        return editarEventoUseCase.ejecutar(id, cambios)
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<EventoResponse>> cancelar(@PathVariable UUID id,
                                                         @Valid @RequestBody CancelarEventoRequest request) {
        return cancelarEventoUseCase.ejecutar(id, request.motivo())
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}

