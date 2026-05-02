package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.PanelItem;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.BloqueoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto.PanelItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BloqueoRestMapper {

    @Mapping(target = "bloqueoId", source = "id")
    @Mapping(target = "asientoIds", expression = "java(java.util.List.of(bloqueo.getAsientoId()))")
    @Mapping(target = "estado", expression = "java(bloqueo.getEstado().name())")
    BloqueoResponse toBloqueoResponse(Bloqueo bloqueo);

    default BloqueoResponse toBloqueoResponseBatch(List<Bloqueo> bloqueos) {
        if (bloqueos.isEmpty()) {
            return null;
        }
        Bloqueo first = bloqueos.get(0);
        List<java.util.UUID> asientoIds = bloqueos.stream()
                .map(Bloqueo::getAsientoId)
                .toList();
        return new BloqueoResponse(first.getId(), asientoIds,
                first.getDestinatario(), first.getEstado().name(), first.getFechaCreacion());
    }

    @Mapping(target = "tipo", expression = "java(panelItem.tipo().name())")
    PanelItemResponse toPanelItemResponse(PanelItem panelItem);
}
