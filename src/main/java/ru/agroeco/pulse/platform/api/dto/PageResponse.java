package ru.agroeco.pulse.platform.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Унифицированный контракт пагинации для всего API.
 *
 * Заменяет прямую сериализацию Spring PageImpl (служебные поля
 * pageable/sort/first/last/numberOfElements/empty) на стабильный набор.
 *
 * Используется во всех модулях: refs, videoserver, incident.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <E, T> PageResponse<T> of(Page<E> p, Function<E, T> mapper) {
        return new PageResponse<>(
                p.getContent().stream().map(mapper).toList(),
                p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(),
                p.hasNext(), p.hasPrevious());
    }

    public static <T> PageResponse<T> of(Page<T> p) {
        return of(p, Function.identity());
    }
}