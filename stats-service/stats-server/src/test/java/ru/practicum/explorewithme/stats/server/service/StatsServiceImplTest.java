package ru.practicum.explorewithme.stats.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.server.mapper.EndpointHitMapper;
import ru.practicum.explorewithme.stats.server.model.EndpointHit;
import ru.practicum.explorewithme.stats.server.repository.StatsRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты реализации сервиса статистики")
class StatsServiceImplTest {

    @Mock // Мок репозитория статистики
    private StatsRepository statsRepository;

    @Mock // Мок маппера EndpointHit
    private EndpointHitMapper endpointHitMapper;

    @InjectMocks // Тестируемый сервис статистики
    private StatsServiceImpl statsService;

    @Captor // Для захвата аргумента при вызове save
    private ArgumentCaptor<EndpointHit> endpointHitArgumentCaptor;

    // Тестовые данные и вспомогательные переменные
    private EndpointHitDto validHitDto;
    private EndpointHit mappedEndpointHit;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        validHitDto = EndpointHitDto.builder()
            .app("test-app")
            .uri("/test-uri")
            .ip("127.0.0.1")
            .timestamp(now.minusHours(1))
            .build();

        mappedEndpointHit = EndpointHit.builder()
            .app(validHitDto.getApp())
            .uri(validHitDto.getUri())
            .ip(validHitDto.getIp())
            .timestamp(validHitDto.getTimestamp())
            .build();
    }

    @Nested
    @DisplayName("Тесты метода saveHit")
    class SaveHitTests {
        @Test
        @DisplayName("Должен успешно сохранить хит при получении валидного DTO")
        void saveHit_whenDtoIsValid_shouldMapAndSave() {
            when(endpointHitMapper.toEndpointHit(validHitDto)).thenReturn(mappedEndpointHit);

            statsService.saveHit(validHitDto);

            verify(endpointHitMapper, times(1)).toEndpointHit(validHitDto);
            verify(statsRepository, times(1)).save(endpointHitArgumentCaptor.capture());

            EndpointHit capturedHit = endpointHitArgumentCaptor.getValue();
            assertThat(capturedHit.getApp()).isEqualTo(validHitDto.getApp());
            assertThat(capturedHit.getUri()).isEqualTo(validHitDto.getUri());
            assertThat(capturedHit.getIp()).isEqualTo(validHitDto.getIp());
            assertThat(capturedHit.getTimestamp()).isEqualTo(validHitDto.getTimestamp());
        }

        @Test
        @DisplayName("Должен выбросить исключение, если DTO равен null")
        void saveHit_whenDtoIsNull_shouldThrowIllegalArgumentException() {
            assertThatThrownBy(() -> statsService.saveHit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Input EndpointHitDto cannot be null");

            verify(endpointHitMapper, never()).toEndpointHit(any());
            verify(statsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить исключение, если маппер вернул null для валидного DTO")
        void saveHit_whenMapperReturnsNull_shouldThrowIllegalStateExceptionOrHandle() {
            EndpointHitDto nonNullDto = validHitDto;
            when(endpointHitMapper.toEndpointHit(nonNullDto)).thenReturn(null);
            // Имитируем, что репозиторий выбросит исключение при попытке сохранить null
            doThrow(new IllegalArgumentException("Entity must not be null")).when(statsRepository).save(null);

            assertThatThrownBy(() -> statsService.saveHit(nonNullDto))
                .isInstanceOf(IllegalArgumentException.class) // Исключение выброшено репозиторием
                .hasMessageContaining("Entity must not be null");

            verify(endpointHitMapper, times(1)).toEndpointHit(nonNullDto);
            verify(statsRepository, times(1)).save(null); // Проверяем, что была попытка сохранить null
        }
    }

    @Nested
    @DisplayName("Тесты метода getStats")
    class GetStatsTests {
        private LocalDateTime start;
        private LocalDateTime end;
        private List<ViewStatsDto> expectedStatsList;

        // Вспомогательные данные для getStats
        @BeforeEach
        void getStatsSetup() {
            start = now.minusDays(1);
            end = now;
            expectedStatsList = List.of(
                ViewStatsDto.builder().app("app1").uri("/uri1").hits(10L).build(),
                ViewStatsDto.builder().app("app2").uri("/uri2").hits(5L).build()
            );
        }

        @Test
        @DisplayName("Должен вызвать findStats, когда unique=false и uris=null")
        void getStats_whenUniqueFalseAndUrisNull_shouldCallFindStats() {
            when(statsRepository.findStats(start, end, null)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, null, false);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findStats(start, end, null);
            verify(statsRepository, never()).findUniqueStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findStats с uris=null, когда unique=false и uris пустой список")
        void getStats_whenUniqueFalseAndUrisEmpty_shouldCallFindStatsWithNullUris() {
            when(statsRepository.findStats(start, end, null)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, Collections.emptyList(), false);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findStats(start, end, null); // Сервис преобразует пустой список в null
            verify(statsRepository, never()).findUniqueStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findStats с указанными uris, когда unique=false")
        void getStats_whenUniqueFalseAndUrisProvided_shouldCallFindStatsWithUris() {
            List<String> uris = List.of("/uri1", "/uri2");
            when(statsRepository.findStats(start, end, uris)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, uris, false);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findStats(start, end, uris);
            verify(statsRepository, never()).findUniqueStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findUniqueStats, когда unique=true и uris=null")
        void getStats_whenUniqueTrueAndUrisNull_shouldCallFindUniqueStats() {
            when(statsRepository.findUniqueStats(start, end, null)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, null, true);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findUniqueStats(start, end, null);
            verify(statsRepository, never()).findStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findUniqueStats с uris=null, когда unique=true и uris пустой список")
        void getStats_whenUniqueTrueAndUrisEmpty_shouldCallFindUniqueStatsWithNullUris() {
            when(statsRepository.findUniqueStats(start, end, null)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, Collections.emptyList(), true);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findUniqueStats(start, end, null); // Сервис преобразует пустой список в null
            verify(statsRepository, never()).findStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findUniqueStats с указанными uris, когда unique=true")
        void getStats_whenUniqueTrueAndUrisProvided_shouldCallFindUniqueStatsWithUris() {
            List<String> uris = List.of("/uri1", "/uri2");
            when(statsRepository.findUniqueStats(start, end, uris)).thenReturn(expectedStatsList);

            List<ViewStatsDto> actualStats = statsService.getStats(start, end, uris, true);

            assertThat(actualStats).isEqualTo(expectedStatsList);
            verify(statsRepository, times(1)).findUniqueStats(start, end, uris);
            verify(statsRepository, never()).findStats(any(), any(), any());
        }

        @Test
        @DisplayName("Должен выбросить IllegalArgumentException, если дата начала после даты окончания")
        void getStats_whenStartIsAfterEnd_shouldReturnEmptyList() {
            LocalDateTime laterStart = now;
            LocalDateTime earlierEnd = now.minusDays(1);

            assertThatThrownBy(() -> statsService.getStats(laterStart, earlierEnd, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error: Start date cannot be after end date.");

            verify(statsRepository, never()).findStats(any(), any(), any());
            verify(statsRepository, never()).findUniqueStats(any(), any(), any());
        }
    }
}