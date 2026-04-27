package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.CrearCortesiaUseCase;
import com.ticketseller.application.GestionarCortesiaUseCase;
import com.ticketseller.domain.model.CategoriaCortesia;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CortesiaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CrearCortesiaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.EditarCortesiaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Cortesías", description = "Endpoints para la gestión de cortesías e invitaciones")
public class CortesiaController {

    private final CrearCortesiaUseCase crearCortesiaUseCase;
    private final ConsultarPanelBloqueosUseCase consultarPanelBloqueosUseCase;
    private final GestionarCortesiaUseCase gestionarCortesiaUseCase;

    @Operation(summary = "Crear una cortesía para un invitado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cortesía creada exitosamente"),
            @ApiResponse(responseCode = "409", description = "Asiento no disponible")
    })
    @PostMapping("/eventos/{eventoId}/cortesias")
    public Mono<ResponseEntity<CortesiaResponse>> crearCortesia(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CrearCortesiaRequest request) {
        CategoriaCortesia categoria = CategoriaCortesia.valueOf(request.categoria().toUpperCase());

        return crearCortesiaUseCase.ejecutar(eventoId, request.destinatario(),
                        categoria, request.asientoId(), request.zonaId())
                .map(c -> new CortesiaResponse(c.getId(), c.getCodigoUnico(),
                        c.getDestinatario(), c.getCategoria().name(),
                        c.getAsientoId(), c.getTicketId(), c.getEstado().name()))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Consultar todas las cortesías de un evento")
    @ApiResponse(responseCode = "200", description = "Lista de cortesías retornada")
    @GetMapping("/eventos/{eventoId}/cortesias")
    public Flux<CortesiaResponse> consultarCortesias(@PathVariable UUID eventoId) {
        return consultarPanelBloqueosUseCase.consultarCortesias(eventoId)
                .map(c -> new CortesiaResponse(c.getId(), c.getCodigoUnico(),
                        c.getDestinatario(), c.getCategoria().name(),
                        c.getAsientoId(), c.getTicketId(), c.getEstado().name()));
    }

    @Operation(summary = "Reasignar una cortesía a otro destinatario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Destinatario actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Cortesía no encontrada")
    })
    @PatchMapping("/cortesias/{cortesiaId}")
    public Mono<ResponseEntity<CortesiaResponse>> editarCortesia(
            @PathVariable UUID cortesiaId,
            @Valid @RequestBody EditarCortesiaRequest request) {
        return gestionarCortesiaUseCase.editarDestinatario(cortesiaId, request.destinatario())
                .map(c -> new CortesiaResponse(c.getId(), c.getCodigoUnico(),
                        c.getDestinatario(), c.getCategoria().name(),
                        c.getAsientoId(), c.getTicketId(), c.getEstado().name()))
                .map(ResponseEntity::ok);
    }
}
