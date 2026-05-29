package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.CrearCortesiaConAsientoUseCase;
import com.ticketseller.application.bloqueos.CrearCortesiaGeneralUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CrearCortesiaGeneralRequest;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CrearCortesiaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CortesiaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CortesiaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/eventos")
@RequiredArgsConstructor
@Tag(name = "Admin - Cortesías", description = "Generación de tickets de cortesía para invitados especiales")
public class CortesiaController {

    private final CrearCortesiaConAsientoUseCase crearCortesiaConAsientoUseCase;
    private final CrearCortesiaGeneralUseCase crearCortesiaGeneralUseCase;
    private final CortesiaRestMapper cortesiaRestMapper;

    @Operation(summary = "Crear un ticket de cortesía con asiento específico asignado")
    @PostMapping("/{eventoId}/cortesias/con-asiento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COORDINADOR_PATROCINIOS')")
    public Mono<ResponseEntity<CortesiaResponse>> crearCortesiaConAsiento(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CrearCortesiaRequest request) {
        return crearCortesiaConAsientoUseCase.ejecutar(eventoId, request.destinatarioId(),
                        request.emailDestinatario(), request.categoria(), request.asientoId())
                .map(cortesiaRestMapper::toCortesiaResponse)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }

    @Operation(summary = "Crear un ticket de cortesía general sin asiento asignado")
    @PostMapping("/{eventoId}/cortesias/general")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COORDINADOR_PATROCINIOS')")
    public Mono<ResponseEntity<CortesiaResponse>> crearCortesiaGeneral(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CrearCortesiaGeneralRequest request) {
        return crearCortesiaGeneralUseCase.ejecutar(eventoId, request.destinatarioId(),
                        request.emailDestinatario(), request.categoria(), request.zonaId())
                .map(cortesiaRestMapper::toCortesiaResponse)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }
}
