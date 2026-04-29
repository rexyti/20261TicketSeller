package com.ticketseller.infrastructure.adapter.out.persistence.postventa.mapper;

import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.postventa.TipoReembolso;
import com.ticketseller.infrastructure.adapter.out.persistence.postventa.ReembolsoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReembolsoPersistenceMapper {
    @Mapping(target = "tipo", expression = "java(reembolso.getTipo() == null ? null : reembolso.getTipo().name())")
    @Mapping(target = "estado", expression = "java(reembolso.getEstado() == null ? null : reembolso.getEstado().name())")
    ReembolsoEntity toEntity(Reembolso reembolso);

    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    Reembolso toDomain(ReembolsoEntity entity);

    default TipoReembolso toTipo(String tipo) {
        return tipo == null ? null : TipoReembolso.valueOf(tipo);
    }

    default EstadoReembolso toEstado(String estado) {
        return estado == null ? null : EstadoReembolso.valueOf(estado);
    }
}

