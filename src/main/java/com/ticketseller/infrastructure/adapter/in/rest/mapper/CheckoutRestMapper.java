package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.application.checkout.ProcesarPagoCommand;
import com.ticketseller.application.checkout.ReservarAsientosCommand;
import com.ticketseller.application.checkout.VentaDetalle;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.TicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.VentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CheckoutRestMapper {

    default ReservarAsientosCommand toCommand(ReservarAsientosRequest request, UUID eventoId, UUID compradorId) {
        return new ReservarAsientosCommand(
                compradorId,
                eventoId,
                request.zonaId(),
                request.cantidad(),
                request.esCortesia(),
                request.asientoIds(),
                request.tipoUsuario()
        );
    }

    ProcesarPagoCommand toCommand(ProcesarPagoRequest request);

    @Mapping(source = "accessDetails.zona", target = "zonaNombre")
    @Mapping(source = "accessDetails.compuerta", target = "compuertaNombre")
    @Mapping(source = "accessDetails.asiento", target = "numeroAsiento")
    TicketResponse toTicketResponse(Ticket ticket);

    VentaResponse toVentaResponse(Venta venta, List<TicketResponse> tickets);

    default VentaResponse toResponse(VentaDetalle ventaDetalle) {
        List<TicketResponse> tickets = ventaDetalle.tickets().stream().map(this::toTicketResponse).toList();
        return toVentaResponse(ventaDetalle.venta(), tickets);
    }
}
