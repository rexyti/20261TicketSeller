package com.ticketseller.infrastructure.adapter.in.rest.promocion;

import com.ticketseller.application.promocion.AplicarDescuentoCarritoUseCase;
import com.ticketseller.application.promocion.CrearCodigosPromocionalesUseCase;
import com.ticketseller.application.promocion.CrearPromocionUseCase;
import com.ticketseller.application.promocion.GestionarEstadoPromocionUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.ActualizarEstadoPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CalcularDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearCodigosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.PromocionResponse;
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
import reactor.core.publisher.Mono;

import java.util.List;

import java.util.UUID;

@Tag(name = "Promociones", description = "Gestión de campañas y descuentos")
@RestController
@RequestMapping("/api/v1/admin/promociones")
@RequiredArgsConstructor
public class PromocionController {

    private final CrearPromocionUseCase crearPromocionUseCase;
    private final GestionarEstadoPromocionUseCase gestionarEstadoPromocionUseCase;
    private final CrearCodigosPromocionalesUseCase crearCodigosPromocionalesUseCase;
    private final AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase;
    private final PromocionRestMapper mapper;

    @Operation(summary = "Crear una nueva promoción o preventa")
    @PostMapping
    public Mono<ResponseEntity<PromocionResponse>> crear(@Valid @RequestBody CrearPromocionRequest request) {
        return crearPromocionUseCase.ejecutar(mapper.toDomain(request))
                .map(mapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Cambiar el estado de una promoción")
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<PromocionResponse>> actualizarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarEstadoPromocionRequest request) {
        return gestionarEstadoPromocionUseCase.ejecutar(id, request.estado())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Generar códigos promocionales para una promoción")
    @PostMapping("/{id}/codigos")
    public Mono<ResponseEntity<List<String>>> crearCodigos(
            @PathVariable UUID id,
            @Valid @RequestBody CrearCodigosRequest request) {
        return crearCodigosPromocionalesUseCase.ejecutar(
                id, request.cantidad(), request.usosMaximosPorCodigo(),
                request.prefijo(), request.fechaFin())
                .map(codigo -> codigo.getCodigo())
                .collectList()
                .map(codigos -> ResponseEntity.status(HttpStatus.CREATED).body(codigos));
    }

    @Operation(summary = "Calcular descuentos aplicables al carrito (incluye validación de acceso a preventa)")
    @PostMapping("/calcular-descuentos")
    public Mono<ResponseEntity<DescuentoAplicadoResponse>> calcularDescuentos(
            @Valid @RequestBody CalcularDescuentoRequest request) {
        return aplicarDescuentoCarritoUseCase.ejecutar(
                request.eventoId(),
                request.tipoUsuario(),
                mapper.toItems(request.items()))
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
