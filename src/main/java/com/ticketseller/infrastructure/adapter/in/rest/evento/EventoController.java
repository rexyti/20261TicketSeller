package com.ticketseller.infrastructure.adapter.in.rest.evento;

import com.ticketseller.application.evento.CancelarEventoUseCase;
import com.ticketseller.application.evento.EditarEventoUseCase;
import com.ticketseller.application.evento.FinalizarEventoUseCase;
import com.ticketseller.application.evento.IniciarEventoUseCase;
import com.ticketseller.application.evento.ListarEventosUseCase;
import com.ticketseller.application.evento.ObtenerEventoUseCase;
import com.ticketseller.application.evento.RegistrarEventoUseCase;
import com.ticketseller.application.postventa.ConsultarReembolsosMasivoUseCase;
import com.ticketseller.domain.model.evento.EstadoEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.CancelarEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.CrearEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.EditarEventoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.EventoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.EventoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Eventos", description = "Gestión de eventos")
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final RegistrarEventoUseCase registrarEventoUseCase;
    private final ListarEventosUseCase listarEventosUseCase;
    private final ObtenerEventoUseCase obtenerEventoUseCase;
    private final EditarEventoUseCase editarEventoUseCase;
    private final CancelarEventoUseCase cancelarEventoUseCase;
    private final IniciarEventoUseCase iniciarEventoUseCase;
    private final FinalizarEventoUseCase finalizarEventoUseCase;
    private final ConsultarReembolsosMasivoUseCase consultarReembolsosMasivoUseCase;
    private final EventoRestMapper eventoRestMapper;
    private final PostVentaRestMapper postVentaRestMapper;

    @Operation(summary = "Registrar un nuevo evento")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROMOTOR_EVENTOS')")
    public Mono<ResponseEntity<EventoResponse>> crear(@Valid @RequestBody CrearEventoRequest request) {
        return registrarEventoUseCase.ejecutar(eventoRestMapper.toDomain(request))
                .map(eventoRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Listar eventos")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Flux<EventoResponse> listar(@RequestParam(required = false) String estado) {
        EstadoEvento filtroEstado = estado == null || estado.isBlank() ? null : EstadoEvento.valueOf(estado.toUpperCase());
        return listarEventosUseCase.ejecutar(filtroEstado).map(eventoRestMapper::toResponse);
    }

    @Operation(summary = "Obtener detalles de un evento")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<EventoResponse>> obtener(@PathVariable UUID id) {
        return obtenerEventoUseCase.ejecutar(id)
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Editar información de un evento")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROMOTOR_EVENTOS')")
    public Mono<ResponseEntity<EventoResponse>> editar(@PathVariable UUID id,
                                                       @RequestBody EditarEventoRequest request) {
        Evento cambios = eventoRestMapper.toDomain(request);
        return editarEventoUseCase.ejecutar(id, cambios)
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Cancelar un evento")
    @DeleteMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROMOTOR_EVENTOS')")
    public Mono<ResponseEntity<EventoResponse>> cancelar(@PathVariable UUID id,
                                                         @Valid @RequestBody CancelarEventoRequest request) {
        return cancelarEventoUseCase.ejecutar(id, request.motivo())
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Marcar un evento como en progreso")
    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROMOTOR_EVENTOS')")
    public Mono<ResponseEntity<EventoResponse>> iniciar(@PathVariable UUID id) {
        return iniciarEventoUseCase.ejecutar(id)
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Marcar un evento como finalizado")
    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PROMOTOR_EVENTOS')")
    public Mono<ResponseEntity<EventoResponse>> finalizar(@PathVariable UUID id) {
        return finalizarEventoUseCase.ejecutar(id)
                .map(eventoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar reembolsos generados por la cancelación masiva de un evento")
    @GetMapping("/{id}/reembolsos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'AGENTE_VENTAS')")
    public Flux<ReembolsoResponse> consultarReembolsos(@PathVariable UUID id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return consultarReembolsosMasivoUseCase.ejecutar(id, page, size)
                .map(postVentaRestMapper::toReembolsoResponse);
    }
}
