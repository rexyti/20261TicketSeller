package com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecintoPersistenceMapper {

    @Mapping(target = "categoria", expression = "java(toCategoriaString(recinto.getCategoria()))")
    RecintoEntity toEntity(Recinto recinto);

    @Mapping(target = "categoria", expression = "java(toCategoriaEnum(entity.getCategoria()))")
    Recinto toDomain(RecintoEntity entity);

    default String toCategoriaString(CategoriaRecinto categoria) {
        return categoria == null ? null : categoria.name();
    }

    default CategoriaRecinto toCategoriaEnum(String categoria) {
        return categoria == null ? null : CategoriaRecinto.valueOf(categoria);
    }
}

