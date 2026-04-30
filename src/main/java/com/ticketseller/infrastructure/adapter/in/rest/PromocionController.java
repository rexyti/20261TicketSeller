package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.promocion.CrearCodigosPromocionalesUseCase;
import com.ticketseller.application.promocion.CrearPromocionUseCase;
import com.ticketseller.application.promocion.GestionarEstadoPromocionUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CambiarEstadoPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CodigoPromocionalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearCodigosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.PromocionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Promociones", description = "Gestión de campañas, preventas y códigos promocionales")
@RestController
@RequestMapping("/api/v1/admin/promociones")
@RequiredArgsConstructor
public class PromocionController {

    private final CrearPromocionUseCase crearPromocionUseCase;
    private final GestionarEstadoPromocionUseCase gestionarEstadoPromocionUseCase;
    private final CrearCodigosPromocionalesUseCase crearCodigosPromocionalesUseCase;
    private final PromocionRestMapper promocionRestMapper;

    @Operation(summary = "Crear promoción")
    @PostMapping
    public Mono<ResponseEntity<PromocionResponse>> crear(@Valid @RequestBody CrearPromocionRequest request) {
        return crearPromocionUseCase.ejecutar(promocionRestMapper.toCommand(request))
                .map(promocionRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Cambiar estado de una promoción")
    @PatchMapping("/{promocionId}/estado")
    public Mono<ResponseEntity<PromocionResponse>> cambiarEstado(@PathVariable UUID promocionId,
                                                                 @Valid @RequestBody CambiarEstadoPromocionRequest request) {
        return gestionarEstadoPromocionUseCase.ejecutar(promocionId, request.estado())
                .map(promocionRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Generar códigos promocionales")
    @PostMapping("/{promocionId}/codigos")
    public Mono<ResponseEntity<Flux<CodigoPromocionalResponse>>> generarCodigos(@PathVariable UUID promocionId,
                                                                                 @Valid @RequestBody CrearCodigosRequest request) {
        Flux<CodigoPromocionalResponse> codigos = crearCodigosPromocionalesUseCase
                .ejecutar(promocionRestMapper.toCommand(promocionId, request))
                .map(promocionRestMapper::toResponse);
        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(codigos));
    }
}

