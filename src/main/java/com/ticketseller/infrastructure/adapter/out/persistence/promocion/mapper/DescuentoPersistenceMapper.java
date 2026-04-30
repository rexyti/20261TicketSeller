package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.DescuentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DescuentoPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(domain.getTipo() == null ? null : domain.getTipo().name())")
    DescuentoEntity toEntity(Descuento domain);

    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    Descuento toDomain(DescuentoEntity entity);

    default TipoDescuento toTipo(String tipo) {
        return tipo == null ? null : TipoDescuento.valueOf(tipo);
    }
}

