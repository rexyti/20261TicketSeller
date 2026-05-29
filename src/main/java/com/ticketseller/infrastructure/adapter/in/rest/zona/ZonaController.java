package com.ticketseller.infrastructure.adapter.in.rest.zona;

import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.zona.dto.CrearZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.zona.dto.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Zonas", description = "Gestión de zonas de un recinto")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recintos")
public class ZonaController {
    private final CrearZonaUseCase crearZonaUseCase;
    private final ListarZonasUseCase listarZonasUseCase;
    private final ZonaRestMapper zonaRestMapper;

    @Operation(summary = "Crear una zona en un recinto")
    @PostMapping("/{recintoId}/zonas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO')")
    public Mono<ResponseEntity<ZonaResponse>> crearZona(@PathVariable UUID recintoId,
                                                        @Valid @RequestBody CrearZonaRequest request) {
        Zona zona = zonaRestMapper.toDomain(request);
        return crearZonaUseCase.ejecutar(recintoId, zona)
                .map(zonaRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Listar zonas de un recinto")
    @GetMapping("/{recintoId}/zonas")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO', 'GESTOR_INVENTARIO', 'PROMOTOR_EVENTOS')")
    public Flux<ZonaResponse> listarZonas(@PathVariable UUID recintoId) {
        return listarZonasUseCase.ejecutar(recintoId).map(zonaRestMapper::toResponse);
    }
}
