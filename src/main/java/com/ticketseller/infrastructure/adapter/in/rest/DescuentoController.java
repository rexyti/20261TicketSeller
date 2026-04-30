package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.promocion.AplicarCodigoPromocionalCarritoUseCase;
import com.ticketseller.application.promocion.CrearDescuentoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.AplicacionDescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.AplicarCodigoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.DescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PromocionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Descuentos", description = "Gestión de reglas de descuento y aplicación de códigos")
@RestController
@RequiredArgsConstructor
public class DescuentoController {

    private final CrearDescuentoUseCase crearDescuentoUseCase;
    private final AplicarCodigoPromocionalCarritoUseCase aplicarCodigoPromocionalCarritoUseCase;
    private final PromocionRestMapper promocionRestMapper;

    @Operation(summary = "Crear descuento para una promoción")
    @PostMapping("/api/v1/admin/promociones/{promocionId}/descuentos")
    public Mono<ResponseEntity<DescuentoResponse>> crearDescuento(@PathVariable UUID promocionId,
                                                                  @Valid @RequestBody CrearDescuentoRequest request) {
        return crearDescuentoUseCase.ejecutar(promocionRestMapper.toCommand(promocionId, request))
                .map(promocionRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Aplicar código promocional al carrito")
    @PostMapping("/api/v1/compras/carrito/aplicar-codigo")
    public Mono<ResponseEntity<AplicacionDescuentoResponse>> aplicarCodigo(@Valid @RequestBody AplicarCodigoRequest request) {
        return aplicarCodigoPromocionalCarritoUseCase.ejecutar(request.ventaId(), request.codigo())
                .map(promocionRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}

