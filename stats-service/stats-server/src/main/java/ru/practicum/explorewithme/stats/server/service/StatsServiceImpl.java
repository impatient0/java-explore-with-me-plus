package ru.practicum.explorewithme.stats.server.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.mapper.EndpointHitMapper;
import ru.practicum.explorewithme.stats.server.model.EndpointHit;
import ru.practicum.explorewithme.stats.server.repository.StatsRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class StatsServiceImpl implements StatsService {

    private final EndpointHitMapper endpointHitMapper;
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        log.debug("Service: Attempting to save hit: {}", endpointHitDto);
        if (endpointHitDto == null) {
            log.warn("Service: Cannot save hit, input EndpointHitDto was null.");
            throw new IllegalArgumentException("Input EndpointHitDto cannot be null.");
        }
        EndpointHit endpointHit = endpointHitMapper.toEndpointHit(endpointHitDto);
        statsRepository.save(endpointHit);
        log.info("Service: Hit saved successfully for app: {}, uri: {}", endpointHit.getApp(), endpointHit.getUri());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        log.debug("Service: Requesting stats with params: start={}, end={}, uris={}, unique={}",
                start, end, uris, unique);

        if (start != null && end != null && start.isAfter(end)) {
            log.warn("Validation error in getStats: Start date {} is after end date {}", start, end);
            throw new IllegalArgumentException("Error: Start date cannot be after end date.");
        }

        Collection<String> urisForRepo = (uris == null || uris.isEmpty()) ? null : uris;

        List<ViewStatsDto> stats;
        if (unique) {
            stats = statsRepository.findUniqueStats(start, end, urisForRepo);
        } else {
            stats = statsRepository.findStats(start, end, urisForRepo);
        }
        log.info("Service: Found {} stats entries.", stats.size());
        return stats;
    }

    @Override
    @Transactional
    public void incrementView(Long eventId, String ipAddress) {
        log.debug("Service: Incrementing view for eventId={} from IP={}", eventId, ipAddress);
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("explore-with-me")
                .uri("/events/" + eventId)
                .ip(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        saveHit(hitDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getViewsForEvent(Long eventId) {
        log.debug("Service: Fetching views for eventId={}", eventId);
        List<String> uris = List.of("/events/" + eventId);
        List<ViewStatsDto> stats = getStats(
                LocalDateTime.now().minusYears(100), // Far past to include all views
                LocalDateTime.now(),
                uris,
                false
        );
        return stats.stream()
                .filter(stat -> stat.getUri().equals("/events/" + eventId))
                .map(ViewStatsDto::getHits)
                .findFirst()
                .orElse(0L);
    }
}