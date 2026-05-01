package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.CodigoPromocionalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CodigoPromocionalPersistenceMapper {

    @Mapping(target = "estado", expression = "java(codigoPromocional.getEstado() == null ? null : codigoPromocional.getEstado().name())")
    CodigoPromocionalEntity toEntity(CodigoPromocional codigoPromocional);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    CodigoPromocional toDomain(CodigoPromocionalEntity entity);

    default EstadoCodigoPromocional toEstado(String estado) {
        return estado == null ? null : EstadoCodigoPromocional.valueOf(estado);
    }
}
