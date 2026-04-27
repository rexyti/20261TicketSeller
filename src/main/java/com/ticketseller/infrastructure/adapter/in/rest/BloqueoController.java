package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.BloquearAsientosUseCase;
import com.ticketseller.application.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.GestionarBloqueoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.BloquearAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.BloqueoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.EditarBloqueoRequest;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Bloqueos", description = "Endpoints para la gestión de bloqueos de asientos para patrocinadores")
public class BloqueoController {

    private final BloquearAsientosUseCase bloquearAsientosUseCase;
    private final GestionarBloqueoUseCase gestionarBloqueoUseCase;
    private final ConsultarPanelBloqueosUseCase consultarPanelBloqueosUseCase;

    @Operation(summary = "Bloquear asientos para un patrocinador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asientos bloqueados exitosamente"),
            @ApiResponse(responseCode = "409", description = "Algún asiento ya está bloqueado u ocupado")
    })
    @PostMapping("/eventos/{eventoId}/bloqueos")
    public Mono<ResponseEntity<List<BloqueoResponse>>> bloquearAsientos(
            @PathVariable UUID eventoId,
            @Valid @RequestBody BloquearAsientosRequest request) {
        return bloquearAsientosUseCase.ejecutar(eventoId, request.asientoIds(),
                        request.destinatario(), request.fechaExpiracion())
                .map(bloqueos -> bloqueos.stream()
                        .map(b -> new BloqueoResponse(b.getId(), b.getAsientoId(), b.getEventoId(),
                                b.getDestinatario(), b.getEstado().name(),
                                b.getFechaCreacion(), b.getFechaExpiracion()))
                        .toList())
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Consultar todos los bloqueos de un evento")
    @ApiResponse(responseCode = "200", description = "Lista de bloqueos retornada")
    @GetMapping("/eventos/{eventoId}/bloqueos")
    public Flux<BloqueoResponse> consultarBloqueos(@PathVariable UUID eventoId,
                                                    @RequestParam(required = false) Boolean soloActivos) {
        var flux = Boolean.TRUE.equals(soloActivos)
                ? consultarPanelBloqueosUseCase.consultarBloqueosActivos(eventoId)
                : consultarPanelBloqueosUseCase.consultarBloqueos(eventoId);

        return flux.map(b -> new BloqueoResponse(b.getId(), b.getAsientoId(), b.getEventoId(),
                b.getDestinatario(), b.getEstado().name(),
                b.getFechaCreacion(), b.getFechaExpiracion()));
    }

    @Operation(summary = "Editar el destinatario de un bloqueo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Destinatario actualizado"),
            @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    @PatchMapping("/bloqueos/{bloqueoId}")
    public Mono<ResponseEntity<BloqueoResponse>> editarBloqueo(
            @PathVariable UUID bloqueoId,
            @Valid @RequestBody EditarBloqueoRequest request) {
        return gestionarBloqueoUseCase.editarDestinatario(bloqueoId, request.destinatario())
                .map(b -> new BloqueoResponse(b.getId(), b.getAsientoId(), b.getEventoId(),
                        b.getDestinatario(), b.getEstado().name(),
                        b.getFechaCreacion(), b.getFechaExpiracion()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Liberar un bloqueo y devolver el asiento a disponible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bloqueo liberado"),
            @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    @DeleteMapping("/bloqueos/{bloqueoId}")
    public Mono<ResponseEntity<Void>> liberarBloqueo(@PathVariable UUID bloqueoId) {
        return gestionarBloqueoUseCase.liberarBloqueo(bloqueoId)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()));
    }
}
