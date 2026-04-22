package com.ticketseller.domain.shared;

import java.util.List;

public record Pagina<T>(List<T> contenido, long totalElementos, int pagina, int size) {

    public int totalPaginas() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElementos / size);
    }
}

