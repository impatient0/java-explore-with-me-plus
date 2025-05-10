package ru.practicum.explorewithme.stats.client;

import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsClient {
    void saveHit(EndpointHitDto endpointHitDto);

    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}
