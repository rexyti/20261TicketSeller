package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.BloquearAsientosUseCase;
import com.ticketseller.application.bloqueos.ConsultarPanelBloqueosUseCase;
import com.ticketseller.application.bloqueos.GestionarBloqueoUseCase;
import com.ticketseller.application.bloqueos.TipoPanelItem;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.BloquearAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.BloqueoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.EditarBloqueoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.PanelItemResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.BloqueoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Bloqueos", description = "Gestión de bloqueos de asientos para patrocinadores")
public class BloqueoController {

    private final BloquearAsientosUseCase bloquearAsientosUseCase;
    private final GestionarBloqueoUseCase gestionarBloqueoUseCase;
    private final ConsultarPanelBloqueosUseCase consultarPanelBloqueosUseCase;
    private final BloqueoRestMapper bloqueoRestMapper;

    @Operation(summary = "Bloquear asientos para un patrocinador")
    @PostMapping("/eventos/{eventoId}/bloqueos")
    public Mono<ResponseEntity<BloqueoResponse>> bloquearAsientos(
            @PathVariable UUID eventoId,
            @Valid @RequestBody BloquearAsientosRequest request) {
        return bloquearAsientosUseCase.ejecutar(eventoId, request.asientoIds(),
                        request.destinatario(), request.fechaExpiracion())
                .map(bloqueoRestMapper::toBloqueoResponseBatch)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }

    @Operation(summary = "Consultar panel de bloqueos y cortesías de un evento")
    @GetMapping("/eventos/{eventoId}/bloqueos")
    public Mono<ResponseEntity<List<PanelItemResponse>>> consultarPanel(
            @PathVariable UUID eventoId,
            @RequestParam(required = false) TipoPanelItem tipo) {
        return consultarPanelBloqueosUseCase.ejecutar(eventoId, tipo)
                .map(bloqueoRestMapper::toPanelItemResponse)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Editar el destinatario de un bloqueo")
    @PatchMapping("/bloqueos/{bloqueoId}")
    public Mono<ResponseEntity<BloqueoResponse>> editarBloqueo(
            @PathVariable UUID bloqueoId,
            @Valid @RequestBody EditarBloqueoRequest request) {
        return gestionarBloqueoUseCase.editarDestinatario(bloqueoId, request.destinatario())
                .map(bloqueoRestMapper::toBloqueoResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Liberar un bloqueo y devolver el asiento a disponible")
    @DeleteMapping("/bloqueos/{bloqueoId}")
    public Mono<ResponseEntity<Void>> liberarBloqueo(@PathVariable UUID bloqueoId) {
        return gestionarBloqueoUseCase.liberarBloqueo(bloqueoId)
                .thenReturn(ResponseEntity.<Void>noContent().build());
    }
}
