package ru.agroeco.pulse.refs.api.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class QuerySupport {

    private static final int MAX_PAGE_SIZE = 100;

    private QuerySupport() {}

    /** Оставляет только разрешённые поля сортировки, капает size, дефолтит направление. */
    public static Pageable sanitize(Pageable pageable, Set<String> allowed,
                                    String defaultField, Sort.Direction defaultDirection) {
        List<Sort.Order> valid = pageable.getSort().stream()
                .filter(o -> allowed.contains(o.getProperty()))
                .toList();
        Sort sort = valid.isEmpty()
                ? Sort.by(defaultDirection, defaultField)
                : Sort.by(valid);
        int size = Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    /** Экранирует %, _ и \ для использования с LIKE ... ESCAPE '\'. */
    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Готовый LIKE-паттерн: '%' + escaped lowercase + '%'. */
    public static String containsPattern(String search) {
        return "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
    }
}