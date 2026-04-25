package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.application.checkout.ProcesarPagoCommand;
import com.ticketseller.application.checkout.ReservarAsientosCommand;
import com.ticketseller.application.checkout.VentaDetalle;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.TicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.VentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CheckoutRestMapper {

    ReservarAsientosCommand toCommand(ReservarAsientosRequest request);

    ProcesarPagoCommand toCommand(ProcesarPagoRequest request);

    @Mapping(target = "estadoReembolso", ignore = true)
    @Mapping(target = "detalleReembolso", ignore = true)
    TicketResponse toTicketResponse(Ticket ticket);

    VentaResponse toVentaResponse(Venta venta, List<TicketResponse> tickets);

    default VentaResponse toResponse(VentaDetalle ventaDetalle) {
        List<TicketResponse> tickets = ventaDetalle.tickets().stream().map(this::toTicketResponse).toList();
        return toVentaResponse(ventaDetalle.venta(), tickets);
    }
}

