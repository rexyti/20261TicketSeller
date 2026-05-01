package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.dto.TicketEstadoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccesoRestMapper {
    @Mapping(source = "id", target = "ticketId")
    @Mapping(source = "accessDetails.categoria", target = "categoria")
    @Mapping(source = "accessDetails.zona", target = "bloque")
    @Mapping(source = "accessDetails.compuerta", target = "coordenadaAcceso")
    @Mapping(source = "accessDetails.fechaEvento", target = "fechaEvento")
    TicketEstadoResponse toResponse(Ticket ticket);
}
