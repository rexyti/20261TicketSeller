package com.ticketseller.infrastructure.adapter.out.persistence.postventa.mapper;

import com.ticketseller.domain.model.postventa.HistorialEstadoTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.infrastructure.adapter.out.persistence.postventa.HistorialEstadoTicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HistorialEstadoTicketPersistenceMapper {
    @Mapping(target = "estadoAnterior",
            expression = "java(historial.getEstadoAnterior() == null ? null : historial.getEstadoAnterior().name())")
    @Mapping(target = "estadoNuevo",
            expression = "java(historial.getEstadoNuevo() == null ? null : historial.getEstadoNuevo().name())")
    HistorialEstadoTicketEntity toEntity(HistorialEstadoTicket historial);

    @Mapping(target = "estadoAnterior", expression = "java(toEstado(entity.getEstadoAnterior()))")
    @Mapping(target = "estadoNuevo", expression = "java(toEstado(entity.getEstadoNuevo()))")
    HistorialEstadoTicket toDomain(HistorialEstadoTicketEntity entity);

    default EstadoTicket toEstado(String estado) {
        return estado == null ? null : EstadoTicket.valueOf(estado);
    }
}

