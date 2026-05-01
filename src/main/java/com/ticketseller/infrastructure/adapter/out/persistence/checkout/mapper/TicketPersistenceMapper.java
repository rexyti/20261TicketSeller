package com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper;

import com.ticketseller.domain.model.ticket.AccessDetails;
import com.ticketseller.domain.model.ticket.CategoriaTicket;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketPersistenceMapper {

    @Mapping(target = "estado", expression = "java(ticket.getEstado() == null ? null : ticket.getEstado().name())")
    @Mapping(target = "categoria", expression = "java(ticket.getAccessDetails() != null && ticket.getAccessDetails().getCategoria() != null ? ticket.getAccessDetails().getCategoria().name() : null)")
    @Mapping(target = "fechaEvento", expression = "java(ticket.getAccessDetails() != null ? ticket.getAccessDetails().getFechaEvento() : null)")
    @Mapping(target = "zonaNombre", expression = "java(ticket.getAccessDetails() != null ? ticket.getAccessDetails().getZona() : null)")
    @Mapping(target = "compuertaNombre", expression = "java(ticket.getAccessDetails() != null ? ticket.getAccessDetails().getCompuerta() : null)")
    TicketEntity toEntity(Ticket ticket);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "accessDetails", expression = "java(toAccessDetails(entity))")
    Ticket toDomain(TicketEntity entity);

    default EstadoTicket toEstado(String estado) {
        return estado == null ? null : EstadoTicket.valueOf(estado);
    }

    default AccessDetails toAccessDetails(TicketEntity entity) {
        if (entity.getCategoria() == null && entity.getZonaNombre() == null
                && entity.getCompuertaNombre() == null && entity.getFechaEvento() == null) {
            return null;
        }
        return AccessDetails.builder()
                .categoria(entity.getCategoria() != null ? CategoriaTicket.valueOf(entity.getCategoria()) : null)
                .zona(entity.getZonaNombre())
                .compuerta(entity.getCompuertaNombre())
                .fechaEvento(entity.getFechaEvento())
                .build();
    }
}
