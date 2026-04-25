package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.HistorialTicket;
import com.ticketseller.infrastructure.adapter.out.persistence.HistorialTicketEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HistorialTicketPersistenceMapper {
    HistorialTicket toDomain(HistorialTicketEntity entity);
    HistorialTicketEntity toEntity(HistorialTicket domain);
}
