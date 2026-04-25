package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.mantenimiento.ConsultarEstructuraRecintoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.RecintoEstructuraResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/recintos")
@RequiredArgsConstructor
@Tag(name = "Control de Acceso", description = "Endpoints para la validación de estructura del recinto")
public class RecintoConsultaController {

    private final ConsultarEstructuraRecintoUseCase consultarEstructuraRecintoUseCase;

    @Operation(summary = "Consultar estructura del recinto", description = "Retorna la lista de bloques y zonas del recinto para validación de coherencia.")
    @ApiResponse(responseCode = "200", description = "Estructura del recinto recuperada exitosamente",
            content = @Content(schema = @Schema(implementation = RecintoEstructuraResponse.class)))
    @ApiResponse(responseCode = "404", description = "Recinto no encontrado")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RecintoEstructuraResponse>> consultarEstructura(@PathVariable UUID id) {
        return consultarEstructuraRecintoUseCase.ejecutar(id)
                .map(ResponseEntity::ok);
    }
}
