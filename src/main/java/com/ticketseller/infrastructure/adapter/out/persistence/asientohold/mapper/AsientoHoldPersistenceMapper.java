package com.ticketseller.infrastructure.adapter.out.persistence.asientohold.mapper;

import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.infrastructure.adapter.out.persistence.asientohold.AsientoHoldEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsientoHoldPersistenceMapper {

    @Mapping(target = "estado", expression = "java(hold.getEstado() == null ? null : hold.getEstado().name())")
    AsientoHoldEntity toEntity(AsientoHold hold);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    AsientoHold toDomain(AsientoHoldEntity entity);

    default EstadoHold toEstado(String estado) {
        return estado == null ? null : EstadoHold.valueOf(estado);
    }
}
