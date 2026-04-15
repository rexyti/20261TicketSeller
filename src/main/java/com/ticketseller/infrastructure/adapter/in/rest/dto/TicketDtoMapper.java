package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TicketDtoMapper {

    TicketDtoMapper INSTANCE = Mappers.getMapper(TicketDtoMapper.class);

    @Mapping(source = "codigoQR", target = "codigoQR")
    TicketResponse toResponse(Ticket ticket);
}
