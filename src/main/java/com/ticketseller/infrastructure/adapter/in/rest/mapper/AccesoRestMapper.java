package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.dto.TicketEstadoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccesoRestMapper {
    @Mapping(source = "id", target = "ticketId")
    @Mapping(source = "accessDetails.categoria", target = "categoria")
    @Mapping(source = "accessDetails.zona", target = "zona")
    @Mapping(source = "accessDetails.compuerta", target = "compuertaAsignada")
    @Mapping(source = "accessDetails.fechaEvento", target = "fechaEvento")
    TicketEstadoResponse toResponse(Ticket ticket);
}
