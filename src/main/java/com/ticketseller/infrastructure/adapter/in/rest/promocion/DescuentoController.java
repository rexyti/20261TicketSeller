package com.ticketseller.infrastructure.adapter.in.rest.promocion;

import com.ticketseller.application.promocion.CrearDescuentoUseCase;
import com.ticketseller.application.promocion.ValidarCodigoPromocionalUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.AplicarCodigoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoResponse;
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

@Tag(name = "Descuentos", description = "Gestión de descuentos y códigos promocionales")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DescuentoController {

    private final CrearDescuentoUseCase crearDescuentoUseCase;
    private final ValidarCodigoPromocionalUseCase validarCodigoPromocionalUseCase;
    private final PromocionRestMapper mapper;

    @Operation(summary = "Crear un descuento para una promoción")
    @PostMapping("/admin/promociones/{id}/descuentos")
    public Mono<ResponseEntity<DescuentoResponse>> crearDescuento(
            @PathVariable UUID id,
            @Valid @RequestBody CrearDescuentoRequest request) {
        return crearDescuentoUseCase.ejecutar(id, mapper.toDomain(request))
                .map(mapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Aplicar un código promocional al carrito")
    @PostMapping("/compras/carrito/aplicar-codigo")
    public Mono<ResponseEntity<DescuentoResponse>> aplicarCodigo(
            @Valid @RequestBody AplicarCodigoRequest request) {
        return validarCodigoPromocionalUseCase.ejecutar(request.codigo())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
