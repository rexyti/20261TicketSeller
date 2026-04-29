package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.application.postventa.CancelacionResultado;
import com.ticketseller.application.postventa.TicketConReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.CancelacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.ReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.TicketConReembolsoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostVentaRestMapper {
    CancelacionResponse toCancelacionResponse(CancelacionResultado resultado);

    @Mapping(source = "id", target = "reembolsoId")
    ReembolsoResponse toReembolsoResponse(Reembolso reembolso);

    default TicketConReembolsoResponse toTicketConReembolsoResponse(TicketConReembolso ticketConReembolso) {
        Ticket ticket = ticketConReembolso.ticket();
        Reembolso reembolso = ticketConReembolso.reembolso();
        return new TicketConReembolsoResponse(
                ticket.getId(),
                ticket.getEstado(),
                reembolso == null ? null : reembolso.getEstado(),
                reembolso == null ? null : reembolso.getMonto(),
                reembolso == null ? null : reembolso.getId()
        );
    }
}

