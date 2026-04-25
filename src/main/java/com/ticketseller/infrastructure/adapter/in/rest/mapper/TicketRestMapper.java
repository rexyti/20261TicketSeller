package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketRestMapper {
    @Mapping(target = "id", source = "ticket.id")
    @Mapping(target = "ventaId", source = "ticket.ventaId")
    @Mapping(target = "eventoId", source = "ticket.eventoId")
    @Mapping(target = "estado", source = "ticket.estado")
    @Mapping(target = "precio", source = "ticket.precio")
    @Mapping(target = "codigoQr", source = "ticket.codigoQr")
    @Mapping(target = "estadoReembolso", source = "reembolso.estado")
    @Mapping(target = "montoReembolso", source = "reembolso.monto")
    @Mapping(target = "fechaSolicitudReembolso", source = "reembolso.fechaSolicitud")
    TicketConReembolsoResponse toResponse(Ticket ticket, Reembolso reembolso);
}
