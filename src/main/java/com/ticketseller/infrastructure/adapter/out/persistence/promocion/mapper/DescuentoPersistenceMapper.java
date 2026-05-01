package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.DescuentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DescuentoPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(descuento.getTipo() == null ? null : descuento.getTipo().name())")
    DescuentoEntity toEntity(Descuento descuento);

    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    Descuento toDomain(DescuentoEntity entity);

    default TipoDescuento toTipo(String tipo) {
        return tipo == null ? null : TipoDescuento.valueOf(tipo);
    }
}
