package com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.mapper;

import com.ticketseller.domain.model.CancelacionEvento;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.CancelacionEventoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CancelacionEventoPersistenceMapper {

    CancelacionEventoEntity toEntity(CancelacionEvento cancelacionEvento);

    CancelacionEvento toDomain(CancelacionEventoEntity entity);
}

