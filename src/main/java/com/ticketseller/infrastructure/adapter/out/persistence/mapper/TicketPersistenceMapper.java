package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.Ticket;
import com.ticketseller.infrastructure.adapter.out.persistence.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketPersistenceMapper {

    @Mapping(target = "codigoQr", source = "codigoQR")
    @Mapping(target = "createdAt", ignore = true)
    TicketEntity toEntity(Ticket ticket);

    @Mapping(target = "codigoQR", source = "codigoQr")
    @Mapping(target = "withEstado", ignore = true)
    @Mapping(target = "withCodigoQR", ignore = true)
    Ticket toDomain(TicketEntity entity);
}
