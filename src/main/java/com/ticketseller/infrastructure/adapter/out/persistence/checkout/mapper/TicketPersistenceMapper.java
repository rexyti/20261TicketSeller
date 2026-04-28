package com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketPersistenceMapper {

    @Mapping(target = "estado", expression = "java(ticket.getEstado() == null ? null : ticket.getEstado().name())")
    TicketEntity toEntity(Ticket ticket);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    Ticket toDomain(TicketEntity entity);

    default EstadoTicket toEstado(String estado) {
        return estado == null ? null : EstadoTicket.valueOf(estado);
    }
}

