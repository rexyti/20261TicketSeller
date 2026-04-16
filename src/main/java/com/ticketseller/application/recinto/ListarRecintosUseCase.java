package com.ticketseller.application.recinto;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

public class ListarRecintosUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public ListarRecintosUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Flux<Recinto> ejecutar() {
        return recintoRepositoryPort.listarTodos().filter(Recinto::isActivo);
    }

    public Mono<Page<Recinto>> ejecutarFiltrado(String nombre,
                                                CategoriaRecinto categoria,
                                                String ciudad,
                                                Boolean activo,
                                                int page,
                                                int size,
                                                String sort) {
        return recintoRepositoryPort.listarTodos()
                .filter(r -> nombre == null || r.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(r -> categoria == null || categoria.equals(r.getCategoria()))
                .filter(r -> ciudad == null || r.getCiudad().equalsIgnoreCase(ciudad))
                .filter(r -> activo == null || activo.equals(r.isActivo()))
                .sort(obtenerComparador(sort))
                .collectList()
                .map(list -> paginar(list, page, size));
    }

    private Comparator<Recinto> obtenerComparador(String sort) {
        if (sort == null || sort.isBlank()) {
            return Comparator.comparing(Recinto::getNombre, String.CASE_INSENSITIVE_ORDER);
        }
        String[] parts = sort.split(",");
        String field = parts[0].trim().toLowerCase();
        boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);
        Comparator<Recinto> comparator = switch (field) {
            case "ciudad" -> Comparator.comparing(Recinto::getCiudad, String.CASE_INSENSITIVE_ORDER);
            case "capacidadmaxima" -> Comparator.comparing(Recinto::getCapacidadMaxima);
            default -> Comparator.comparing(Recinto::getNombre, String.CASE_INSENSITIVE_ORDER);
        };
        return desc ? comparator.reversed() : comparator;
    }

    private Page<Recinto> paginar(List<Recinto> list, int page, int size) {
        int currentPage = Math.max(0, page);
        int pageSize = size <= 0 ? 10 : size;
        int start = currentPage * pageSize;
        if (start >= list.size()) {
            return new PageImpl<>(List.of(), PageRequest.of(currentPage, pageSize), list.size());
        }
        int end = Math.min(start + pageSize, list.size());
        return new PageImpl<>(list.subList(start, end), PageRequest.of(currentPage, pageSize), list.size());
    }
}
