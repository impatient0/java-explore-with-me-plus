package ru.practicum.explorewithme.stats.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("Тесты для StatsClientImpl")
class StatsClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private StatsClientImpl statsClient;
    private final String baseUrl = "http://stats-server:9090";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        // Создаем RestClient на основе RestTemplate
        RestClient restClient = RestClient.builder(restTemplate)
                .baseUrl(baseUrl)
                .build();

        statsClient = new StatsClientImpl(restClient);
    }

    @Nested
    @DisplayName("Тесты метода saveHit")
    class SaveHitTests {
        @Test
        @DisplayName("Успешное сохранение статистики")
        void saveHit_successful() {
            // Подготовка тестовых данных
            LocalDateTime timestamp = LocalDateTime.now();
            EndpointHitDto hitDto = new EndpointHitDto(
                    "service",
                    "/test",
                    "192.168.0.1",
                    timestamp
            );

            // Настройка ожидания и ответа сервера
            mockServer.expect(requestTo(baseUrl + "/hit"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.app").value("service"))
                    .andExpect(jsonPath("$.uri").value("/test"))
                    .andExpect(jsonPath("$.ip").value("192.168.0.1"))
                    .andRespond(withStatus(HttpStatus.CREATED));

            // Вызов тестируемого метода
            assertDoesNotThrow(
                    () -> statsClient.saveHit(hitDto),
                    "Метод saveHit должен успешно выполниться без исключений"
            );

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Обработка ошибки сервера при сохранении")
        void saveHit_throwsExceptionWhenFails() {
            // Подготовка тестовых данных
            EndpointHitDto hitDto = new EndpointHitDto(
                    "service",
                    "/test",
                    "192.168.0.1",
                    LocalDateTime.now()
            );

            // Настройка ожидания и ответа сервера с ошибкой 500
            mockServer.expect(requestTo(baseUrl + "/hit"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());

            // Вызов тестируемого метода и проверка исключения
            Exception exception = assertThrows(
                    RestClientException.class,
                    () -> statsClient.saveHit(hitDto),
                    "Должно быть выброшено исключение при ошибке сервера"
            );

            assertThat(exception.getMessage())
                    .as("Сообщение об ошибке должно содержать информацию о проблеме")
                    .contains("500");

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Обработка клиентской ошибки (400 Bad Request)")
        void saveHit_handlesBadRequest() {
            // Подготовка тестовых данных с некорректными значениями
            EndpointHitDto hitDto = new EndpointHitDto(
                    "", // Пустое значение приведет к ошибке валидации
                    "/test",
                    "192.168.0.1",
                    LocalDateTime.now()
            );

            // Настройка ожидания и ответа сервера с ошибкой 400
            mockServer.expect(requestTo(baseUrl + "/hit"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Validation failed\"}"));

            // Вызов тестируемого метода и проверка исключения
            Exception exception = assertThrows(
                    RestClientException.class,
                    () -> statsClient.saveHit(hitDto),
                    "Должно быть выброшено исключение при ошибке валидации"
            );

            assertThat(exception.getMessage())
                    .as("Сообщение об ошибке должно содержать информацию о коде статуса")
                    .contains("400");

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Обработка null в качестве параметра")
        void saveHit_handlesNullParameter() {
            // Вызов метода с null-параметром
            Exception exception = assertThrows(
                    NullPointerException.class,
                    () -> statsClient.saveHit(null),
                    "Должно быть выброшено исключение при null-параметре"
            );

            // Проверка, что мок-сервер не получил запроса
            // Это означает, что исключение произошло до обращения к серверу
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("Тесты метода getStats")
    class GetStatsTests {
        @Test
        @DisplayName("Успешное получение статистики")
        void getStats_successful() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = Arrays.asList("/event/1", "/event/2");
            Boolean unique = true;

            String expectedResponseJson =
                    "[{\"app\":\"app1\",\"uri\":\"/event/1\",\"hits\":10}," +
                            "{\"app\":\"app1\",\"uri\":\"/event/2\",\"hits\":5}]";

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // Формирование URL для запроса
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&uris=/event/1" +
                    "&uris=/event/2" +
                    "&unique=" + unique;

            // Настройка ожидания и ответа сервера
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

            // Вызов тестируемого метода
            List<ViewStatsDto> result = statsClient.getStats(start, end, uris, unique);

            // Проверка результата
            assertThat(result)
                    .as("Результат должен содержать 2 элемента")
                    .hasSize(2);

            assertThat(result.get(0).getApp())
                    .as("Первый элемент должен иметь правильное значение app")
                    .isEqualTo("app1");

            assertThat(result.get(0).getUri())
                    .as("Первый элемент должен иметь правильное значение uri")
                    .isEqualTo("/event/1");

            assertThat(result.get(0).getHits())
                    .as("Первый элемент должен иметь правильное значение hits")
                    .isEqualTo(10L);

            assertThat(result.get(1).getUri())
                    .as("Второй элемент должен иметь правильное значение uri")
                    .isEqualTo("/event/2");

            assertThat(result.get(1).getHits())
                    .as("Второй элемент должен иметь правильное значение hits")
                    .isEqualTo(5L);

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Получение статистики с пустым списком URI")
        void getStats_withEmptyUris() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = Collections.emptyList();
            Boolean unique = false;

            String expectedResponseJson = "[{\"app\":\"app1\",\"uri\":\"all\",\"hits\":15}]";

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // Формирование URL для запроса
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&unique=" + unique;

            // Настройка ожидания и ответа сервера
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

            // Вызов тестируемого метода
            List<ViewStatsDto> result = statsClient.getStats(start, end, uris, unique);

            // Проверка результата
            assertThat(result)
                    .as("Результат должен содержать 1 элемент")
                    .hasSize(1);

            assertThat(result.get(0).getApp())
                    .as("Элемент должен иметь правильное значение app")
                    .isEqualTo("app1");

            assertThat(result.get(0).getUri())
                    .as("Элемент должен иметь правильное значение uri")
                    .isEqualTo("all");

            assertThat(result.get(0).getHits())
                    .as("Элемент должен иметь правильное значение hits")
                    .isEqualTo(15L);

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Получение статистики с null вместо списка URI")
        void getStats_withNullUris() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = null;
            Boolean unique = false;

            String expectedResponseJson = "[{\"app\":\"app1\",\"uri\":\"all\",\"hits\":15}]";

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // URL без параметров uris
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&unique=" + unique;

            // Настройка ожидания и ответа сервера
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

            // Вызов тестируемого метода
            List<ViewStatsDto> result = statsClient.getStats(start, end, uris, unique);

            // Проверка результата
            assertThat(result)
                    .as("Результат должен содержать 1 элемент")
                    .hasSize(1);

            assertThat(result.get(0).getUri())
                    .as("Элемент должен иметь правильное значение uri")
                    .isEqualTo("all");

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Получение статистики с null вместо флага unique")
        void getStats_withNullUniqueFlag() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = Collections.singletonList("/event/1");
            Boolean unique = null;

            String expectedResponseJson = "[{\"app\":\"app1\",\"uri\":\"/event/1\",\"hits\":10}]";

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // URL с null в качестве unique
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&uris=/event/1";

            // Настройка ожидания и ответа сервера
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(expectedResponseJson, MediaType.APPLICATION_JSON));

            // Вызов тестируемого метода
            List<ViewStatsDto> result = statsClient.getStats(start, end, uris, unique);

            // Проверка результата
            assertThat(result)
                    .as("Результат должен содержать 1 элемент")
                    .hasSize(1);

            assertThat(result.get(0).getUri())
                    .as("Элемент должен иметь правильное значение uri")
                    .isEqualTo("/event/1");

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Обработка ошибки сервера при получении статистики")
        void getStats_throwsExceptionWhenFails() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = Arrays.asList("/event/1", "/event/2");
            Boolean unique = true;

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // Формирование URL для запроса
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&uris=/event/1" +
                    "&uris=/event/2" +
                    "&unique=" + unique;

            // Настройка ожидания и ответа сервера с ошибкой
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withServerError());

            // Вызов тестируемого метода и проверка исключения
            Exception exception = assertThrows(
                    RestClientException.class,
                    () -> statsClient.getStats(start, end, uris, unique),
                    "Должно быть выброшено исключение при ошибке сервера"
            );

            assertThat(exception.getMessage())
                    .as("Сообщение об ошибке должно содержать информацию о проблеме")
                    .contains("500");

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }

        @Test
        @DisplayName("Получение пустого массива в ответе")
        void getStats_emptyResponse() {
            // Подготовка тестовых данных с использованием LocalDateTime
            LocalDateTime start = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
            LocalDateTime end = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
            List<String> uris = Arrays.asList("/non-existent/1", "/non-existent/2");
            Boolean unique = true;

            // Форматированные строки для URL
            String formattedStart = "2023-01-01%2000:00:00";
            String formattedEnd = "2023-12-31%2023:59:59";

            // Формирование URL
            String url = baseUrl + "/stats" +
                    "?start=" + formattedStart +
                    "&end=" + formattedEnd +
                    "&uris=/non-existent/1" +
                    "&uris=/non-existent/2" +
                    "&unique=" + unique;

            // Настройка ожидания и ответа сервера с пустым массивом
            mockServer.expect(requestTo(url))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

            // Вызов тестируемого метода
            List<ViewStatsDto> result = statsClient.getStats(start, end, uris, unique);

            // Проверка, что результат - пустой список
            assertThat(result)
                    .as("Результат должен быть пустым списком")
                    .isEmpty();

            // Проверка выполнения всех ожиданий
            mockServer.verify();
        }
    }
}