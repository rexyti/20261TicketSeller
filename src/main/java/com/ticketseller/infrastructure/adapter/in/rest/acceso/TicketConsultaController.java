package com.ticketseller.infrastructure.adapter.in.rest.acceso;

import com.ticketseller.application.checkout.ConsultarEstadoTicketUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.dto.TicketEstadoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AccesoRestMapper;
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
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Control de Acceso", description = "Endpoints para la validación de tickets y acceso al recinto")
public class TicketConsultaController {

    private final ConsultarEstadoTicketUseCase consultarEstadoTicketUseCase;
    private final AccesoRestMapper accesoRestMapper;

    @Operation(summary = "Consultar estado del ticket", description = "Retorna el estado actual, zona, categoría y metadatos del ticket para validación de acceso.")
    @ApiResponse(responseCode = "200", description = "Estado del ticket recuperado exitosamente",
            content = @Content(schema = @Schema(implementation = TicketEstadoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Ticket no encontrado")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<TicketEstadoResponse>> consultarEstado(@PathVariable UUID id) {
        return consultarEstadoTicketUseCase.ejecutar(id)
                .map(accesoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
