package com.ticketseller.infrastructure.adapter.in.rest.bloqueos;

import com.ticketseller.application.bloqueos.CrearCortesiaUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CrearCortesiaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.CortesiaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CortesiaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Cortesías", description = "Generación de tickets de cortesía para invitados especiales")
public class CortesiaController {

    private final CrearCortesiaUseCase crearCortesiaUseCase;
    private final CortesiaRestMapper cortesiaRestMapper;

    @Operation(summary = "Crear un ticket de cortesía con o sin asiento asignado")
    @PostMapping("/eventos/{eventoId}/cortesias")
    public Mono<ResponseEntity<CortesiaResponse>> crearCortesia(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CrearCortesiaRequest request) {
        return crearCortesiaUseCase.ejecutar(eventoId, request.destinatario(),
                        request.categoria(), request.asientoId())
                .map(cortesiaRestMapper::toCortesiaResponse)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }
}
