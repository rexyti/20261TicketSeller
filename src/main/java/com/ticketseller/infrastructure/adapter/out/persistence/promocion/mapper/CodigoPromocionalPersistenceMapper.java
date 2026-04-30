package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.CodigoPromocionalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CodigoPromocionalPersistenceMapper {

    @Mapping(target = "estado", expression = "java(domain.getEstado() == null ? null : domain.getEstado().name())")
    @Mapping(target = "usosActuales", expression = "java(domain.getUsosActuales())")
    CodigoPromocionalEntity toEntity(CodigoPromocional domain);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "usosActuales", expression = "java(entity.getUsosActuales() == null ? 0 : entity.getUsosActuales())")
    CodigoPromocional toDomain(CodigoPromocionalEntity entity);

    default EstadoCodigoPromocional toEstado(String estado) {
        return estado == null ? null : EstadoCodigoPromocional.valueOf(estado);
    }
}

