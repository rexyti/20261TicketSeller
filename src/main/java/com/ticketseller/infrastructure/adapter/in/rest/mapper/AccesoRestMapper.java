package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.acceso.dto.TicketEstadoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccesoRestMapper {
    @Mapping(source = "id", target = "ticketId")
    TicketEstadoResponse toResponse(Ticket ticket);
}
