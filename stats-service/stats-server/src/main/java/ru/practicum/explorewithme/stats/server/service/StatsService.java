package ru.practicum.explorewithme.stats.server.service;

import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    /**
     * Сохраняет информацию о запросе к эндпоинту.
     *
     * @param endpointHitDto DTO с данными о запросе.
     */
    void saveHit(EndpointHitDto endpointHitDto);

    /**
     * Получает статистику по посещениям эндпоинтов.
     *
     * @param start  Начало периода статистики.
     * @param end    Конец периода статистики.
     * @param uris   Список URI для фильтрации статистики.
     * @param unique Флаг уникальности IP-адресов.
     * @return Список DTO с данными статистики.
     */
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}