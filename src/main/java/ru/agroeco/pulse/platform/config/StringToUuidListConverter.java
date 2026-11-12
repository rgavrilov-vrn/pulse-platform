package ru.agroeco.pulse.platform.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Устойчивый конвертер строки в список UUID.
 * Обрабатывает:
 *  - обычный список через запятую: "uuid1,uuid2"
 *  - JSON-подобный массив: '["uuid1","uuid2"]' или "[uuid1,uuid2]"
 *  - одиночное значение: "uuid1"
 *  - пустую строку (возвращает пустой список)
 */
@Component
public class StringToUuidListConverter implements Converter<String, List<UUID>> {

    @Override
    public List<UUID> convert(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        // Удаляем квадратные скобки и любые кавычки (одинарные/двойные)
        String cleaned = source.replaceAll("[\\[\\]\"']", "");

        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }
}