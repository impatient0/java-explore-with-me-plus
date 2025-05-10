package ru.practicum.explorewithme.stats.server.service;

import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    /**
     * Сохраняет информацию о запросе к эндпоинту.
     *
     * @param endpointHitDto DTO с информацией о запросе.
     */
    void saveHit(EndpointHitDto endpointHitDto);

    /**
     * Возвращает статистику по посещениям за указанный период.
     *
     * @param start  дата и время начала диапазона для статистики.
     * @param end    дата и время конца диапазона для статистики.
     * @param uris   список URI, для которых нужна статистика (может быть null или пустым для всех URI).
     * @param unique true, если нужны только уникальные по IP посещения, false иначе.
     * @return список DTO {@link ViewStatsDto} со статистикой.
     */
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);

}