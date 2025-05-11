// CHECKSTYLE:OFF RegexpSinglelineJava
package ru.practicum.explorewithme.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для ViewStatsDto")
class ViewStatsDtoTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Сериализация и десериализация")
    class SerializationTests {
        @Test
        @DisplayName("Корректная сериализация в JSON")
        void testSerializationToJson() throws Exception {
            // Подготовка тестовых данных
            ViewStatsDto dto = new ViewStatsDto("test-service", "/events/1", 100L);

            // Сериализация в JSON
            String json = objectMapper.writeValueAsString(dto);

            // Проверки с информативными сообщениями
            assertThat(json)
                    .as("JSON должен содержать поле app с правильным значением")
                    .contains("\"app\":\"test-service\"");

            assertThat(json)
                    .as("JSON должен содержать поле uri с правильным значением")
                    .contains("\"uri\":\"/events/1\"");

            assertThat(json)
                    .as("JSON должен содержать поле hits с правильным значением")
                    .contains("\"hits\":100");
        }

        @Test
        @DisplayName("Корректная десериализация из JSON")
        void testDeserializationFromJson() throws Exception {
            // Подготовка JSON
            String json = """
                    {
                        "app": "test-service",
                        "uri": "/events/1",
                        "hits": 100
                    }""";

            // Десериализация из JSON
            ViewStatsDto dto = objectMapper.readValue(json, ViewStatsDto.class);

            // Проверки с информативными сообщениями
            assertThat(dto.getApp())
                    .as("Поле app должно быть правильно десериализовано")
                    .isEqualTo("test-service");

            assertThat(dto.getUri())
                    .as("Поле uri должно быть правильно десериализовано")
                    .isEqualTo("/events/1");

            assertThat(dto.getHits())
                    .as("Поле hits должно быть правильно десериализовано")
                    .isEqualTo(100L);
        }

        @Test
        @DisplayName("Десериализация с отсутствующими полями")
        void testDeserializationWithMissingFields() throws Exception {
            // JSON с отсутствующими полями
            String json = """
                    {
                        "app": "test-service"
                    }""";

            ViewStatsDto dto = objectMapper.readValue(json, ViewStatsDto.class);

            // Проверки
            assertThat(dto.getApp())
                    .as("Поле app должно быть правильно десериализовано")
                    .isEqualTo("test-service");

            assertThat(dto.getUri())
                    .as("Поле uri должно быть null при отсутствии в JSON")
                    .isNull();

            assertThat(dto.getHits())
                    .as("Поле hits должно быть null при отсутствии в JSON")
                    .isNull();
        }

        @Test
        @DisplayName("Обработка null-значений")
        void testNullValues() throws Exception {
            // Создание объекта с null-значениями
            ViewStatsDto dto = new ViewStatsDto(null, null, null);

            // Сериализация
            String json = objectMapper.writeValueAsString(dto);

            // Десериализация
            ViewStatsDto deserializedDto = objectMapper.readValue(json, ViewStatsDto.class);

            // Проверки
            assertThat(deserializedDto.getApp()).isNull();
            assertThat(deserializedDto.getUri()).isNull();
            assertThat(deserializedDto.getHits()).isNull();
        }

        @Test
        @DisplayName("Ошибка при неверном типе данных (hits не число)")
        void testInvalidHitsType() {
            // JSON с неверным типом поля hits
            String json = """
                    {
                        "app": "test-service",
                        "uri": "/events/1",
                        "hits": "not-a-number"
                    }""";

            // Проверка исключения при неверном типе
            Exception exception = assertThrows(
                    Exception.class,
                    () -> objectMapper.readValue(json, ViewStatsDto.class),
                    "Должно быть выброшено исключение при неверном типе поля hits"
            );

            assertThat(exception.getMessage())
                    .as("Сообщение об ошибке должно указывать на проблему с типом hits")
                    .contains("hits");
        }
    }

    @Nested
    @DisplayName("Тесты конструкторов и методов")
    class ConstructorTests {
        @Test
        @DisplayName("Конструктор со всеми параметрами")
        void testAllArgsConstructor() {
            // Создание объекта через конструктор со всеми аргументами
            ViewStatsDto dto = new ViewStatsDto("app-name", "/uri", 200L);

            // Проверки
            assertThat(dto.getApp()).isEqualTo("app-name");
            assertThat(dto.getUri()).isEqualTo("/uri");
            assertThat(dto.getHits()).isEqualTo(200L);
        }

        @Test
        @DisplayName("Конструктор без аргументов")
        void testNoArgsConstructor() {
            // Создание объекта через конструктор без аргументов
            ViewStatsDto dto = new ViewStatsDto();

            // Проверки
            assertThat(dto.getApp()).isNull();
            assertThat(dto.getUri()).isNull();
            assertThat(dto.getHits()).isNull();
        }

        @Test
        @DisplayName("Сеттеры")
        void testSetters() {
            // Создание объекта через конструктор без аргументов
            ViewStatsDto dto = new ViewStatsDto();

            // Установка значений через сеттеры
            dto.setApp("app-from-setter");
            dto.setUri("/uri-from-setter");
            dto.setHits(300L);

            // Проверки
            assertThat(dto.getApp()).isEqualTo("app-from-setter");
            assertThat(dto.getUri()).isEqualTo("/uri-from-setter");
            assertThat(dto.getHits()).isEqualTo(300L);
        }

        @Test
        @DisplayName("Equals и HashCode")
        void testEqualsAndHashCode() {
            // Создание двух одинаковых объектов
            ViewStatsDto dto1 = new ViewStatsDto("same-app", "/same-uri", 100L);
            ViewStatsDto dto2 = new ViewStatsDto("same-app", "/same-uri", 100L);

            // Проверка equals
            assertThat(dto1)
                    .as("Объекты с одинаковыми полями должны быть равны")
                    .isEqualTo(dto2);

            // Проверка hashCode
            assertThat(dto1.hashCode())
                    .as("Хеш-коды объектов с одинаковыми полями должны совпадать")
                    .isEqualTo(dto2.hashCode());

            // Изменяем один из объектов
            dto2.setHits(200L);

            // Проверка, что equals и hashCode различаются
            assertThat(dto1)
                    .as("Объекты с разными полями не должны быть равны")
                    .isNotEqualTo(dto2);

            assertThat(dto1.hashCode())
                    .as("Хеш-коды объектов с разными полями не должны совпадать")
                    .isNotEqualTo(dto2.hashCode());
        }
    }
}