package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.CrearZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recintos")
public class ZonaController {
    private final CrearZonaUseCase crearZonaUseCase;
    private final ListarZonasUseCase listarZonasUseCase;
    private final ZonaRestMapper zonaRestMapper;

    @PostMapping("/{recintoId}/zonas")
    public Mono<ResponseEntity<ZonaResponse>> crearZona(@PathVariable UUID recintoId,
                                                        @Valid @RequestBody CrearZonaRequest request) {
        Zona zona = zonaRestMapper.toDomain(request);
        return crearZonaUseCase.ejecutar(recintoId, zona)
                .map(zonaRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @GetMapping("/{recintoId}/zonas")
    public Flux<ZonaResponse> listarZonas(@PathVariable UUID recintoId) {
        return listarZonasUseCase.ejecutar(recintoId).map(zonaRestMapper::toResponse);
    }
}
