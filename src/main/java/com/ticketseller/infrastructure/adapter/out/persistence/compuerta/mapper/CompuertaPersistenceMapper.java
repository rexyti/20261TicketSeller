package com.ticketseller.infrastructure.adapter.out.persistence.compuerta.mapper;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.CompuertaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompuertaPersistenceMapper {

    CompuertaEntity toEntity(Compuerta compuerta);

    Compuerta toDomain(CompuertaEntity entity);
}

