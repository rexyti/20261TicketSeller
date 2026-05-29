package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.checkout.ConsultarTodosTicketsUseCase;
import com.ticketseller.application.postventa.ConsultarEstadoReembolsoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.TicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.TicketConReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CheckoutRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import com.ticketseller.infrastructure.config.SecurityContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Tag(name = "Postventa - Mis Compras", description = "Consulta del estado de reembolsos del comprador")
public class MisComprasController {
    private final ConsultarEstadoReembolsoUseCase consultarEstadoReembolsoUseCase;
    private final ConsultarTodosTicketsUseCase consultarTodosTicketsUseCase;
    private final PostVentaRestMapper postVentaRestMapper;
    private final CheckoutRestMapper checkoutRestMapper;

    @Operation(summary = "Consultar tickets cancelados/reembolsados del comprador autenticado")
    @GetMapping("/mis-compras")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPRADOR')")
    public Flux<TicketConReembolsoResponse> misCompras() {
        return SecurityContextUtils.getUsuarioId()
                .flatMapMany(compradorId -> consultarEstadoReembolsoUseCase.ejecutar(compradorId)
                        .map(postVentaRestMapper::toTicketConReembolsoResponse));
    }

    @Operation(summary = "Consultar todos los tickets del comprador autenticado")
    @GetMapping("/mis-tickets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPRADOR')")
    public Flux<TicketResponse> misTickets() {
        return SecurityContextUtils.getUsuarioId()
                .flatMapMany(compradorId -> consultarTodosTicketsUseCase.ejecutar(compradorId)
                        .map(checkoutRestMapper::toTicketResponse));
    }
}
