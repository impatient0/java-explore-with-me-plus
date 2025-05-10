package ru.practicum.explorewithme.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.util.*;

@Service
@Slf4j
public class StatsClientImpl implements StatsClient {

    private final RestClient restClient;

    public StatsClientImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void saveHit(EndpointHitDto endpointHitDto) {
        try {
            restClient.post()
                    .uri("/hit") // Правильный путь без добавления serverUrl
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                                String errorMsg = "Ошибка при сохранении статистики. Код статуса: " + response.getStatusCode();
                                log.error(errorMsg);
                                throw new RestClientException(errorMsg);
                            }
                    )
                    .onStatus(
                            HttpStatusCode::is2xxSuccessful,
                            (request, response) -> {
                                log.info("Статистика успешно сохранена. Код статуса: {}", response.getStatusCode());
                            }
                    )
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Ошибка при сохранении статистики: {}", e.getMessage());
            throw e; // Перебрасываем исключение дальше
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/stats") // Правильный путь
                                .queryParam("start", start)
                                .queryParam("end", end);

                        if (uris != null && !uris.isEmpty()) {
                            for (String uri : uris) {
                                uriBuilder.queryParam("uris", uri);
                            }
                        }

                        if (unique != null) {
                            uriBuilder.queryParam("unique", unique);
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            (request, response) -> {
                                String errorMsg = "Ошибка при получении статистики. Код статуса: " + response.getStatusCode();
                                log.error(errorMsg);
                                throw new RestClientException(errorMsg);
                            }
                    )
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            String errorMsg = "Ошибка при получении статистики: "+ e.getMessage();
            log.error(errorMsg);
            throw new RestClientException(errorMsg);
        }
    }
}
