package ru.practicum.explorewithme.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Тесты для EndpointHitDto")
class EndpointHitDtoTest {
    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("Сериализация и десериализация")
    class SerializationTests {
        @Test
        @DisplayName("Корректная сериализация в JSON")
        void testSerializationToJson() throws Exception {
            // Подготовка тестовых данных
            LocalDateTime timestamp = LocalDateTime.of(2024, 3, 15, 12, 30, 0);
            EndpointHitDto dto = new EndpointHitDto(
                    "test-app",
                    "/test/path",
                    "192.168.1.1",
                    timestamp
            );

            // Сериализация в JSON
            String json = objectMapper.writeValueAsString(dto);

            // Проверки с информативными сообщениями
            assertThat(json)
                    .as("JSON должен содержать поле app с правильным значением")
                    .contains("\"app\":\"test-app\"");

            assertThat(json)
                    .as("JSON должен содержать поле uri с правильным значением")
                    .contains("\"uri\":\"/test/path\"");

            assertThat(json)
                    .as("JSON должен содержать поле ip с правильным значением")
                    .contains("\"ip\":\"192.168.1.1\"");

            assertThat(json)
                    .as("JSON должен содержать поле timestamp в формате yyyy-MM-dd HH:mm:ss")
                    .contains("\"timestamp\":\"2024-03-15 12:30:00\"");
        }

        @Test
        @DisplayName("Корректная десериализация из JSON")
        void testDeserializationFromJson() throws Exception {
            // Подготовка JSON
            String json = """
                    {
                        "app": "test-app",
                        "uri": "/test/path",
                        "ip": "192.168.1.1",
                        "timestamp": "2024-03-15 12:30:00"
                    }""";

            // Десериализация из JSON
            EndpointHitDto dto = objectMapper.readValue(json, EndpointHitDto.class);

            // Проверки с информативными сообщениями
            assertThat(dto.getApp())
                    .as("Поле app должно быть правильно десериализовано")
                    .isEqualTo("test-app");

            assertThat(dto.getUri())
                    .as("Поле uri должно быть правильно десериализовано")
                    .isEqualTo("/test/path");

            assertThat(dto.getIp())
                    .as("Поле ip должно быть правильно десериализовано")
                    .isEqualTo("192.168.1.1");

            assertThat(dto.getTimestamp())
                    .as("Поле timestamp должно быть правильно десериализовано")
                    .isEqualTo(LocalDateTime.of(2024, 3, 15, 12, 30, 0));
        }

        @Test
        @DisplayName("Ошибка при неверном формате даты")
        void testInvalidTimestampFormat() {
            // Подготовка JSON с неверным форматом даты
            String json = """
                    {
                        "app": "test-app",
                        "uri": "/test/path",
                        "ip": "192.168.1.1",
                        "timestamp": "2024-03-15T12:30:00"
                    }""";

            // Проверка исключения при неверном формате
            Exception exception = assertThrows(
                    Exception.class,
                    () -> objectMapper.readValue(json, EndpointHitDto.class),
                    "Должно быть выброшено исключение при неверном формате даты"
            );

            assertThat(exception.getMessage())
                    .as("Сообщение об ошибке должно указывать на проблему с форматом даты")
                    .contains("timestamp");
        }

        @Test
        @DisplayName("Десериализация с дополнительными полями")
        void testDeserializationWithExtraFields() throws Exception {
            // JSON с дополнительными полями
            String json = """
                    {
                        "app": "test-app",
                        "uri": "/test/path",
                        "ip": "192.168.1.1",
                        "timestamp": "2024-03-15 12:30:00",
                        "extraField": "extra value"
                    }""";

            // Десериализация
            EndpointHitDto dto = objectMapper.readValue(json, EndpointHitDto.class);

            // Проверки основных полей - должны быть правильно заполнены
            assertThat(dto.getApp()).isEqualTo("test-app");
            assertThat(dto.getUri()).isEqualTo("/test/path");
            assertThat(dto.getIp()).isEqualTo("192.168.1.1");
            assertThat(dto.getTimestamp()).isEqualTo(LocalDateTime.of(2024, 3, 15, 12, 30, 0));
        }

        @Test
        @DisplayName("Обработка null-значений")
        void testNullValues() throws Exception {
            // Создание объекта с null-значениями
            EndpointHitDto dto = new EndpointHitDto(null, null, null, null);

            // Сериализация
            String json = objectMapper.writeValueAsString(dto);

            // Десериализация
            EndpointHitDto deserializedDto = objectMapper.readValue(json, EndpointHitDto.class);

            // Проверки
            assertThat(deserializedDto.getApp()).isNull();
            assertThat(deserializedDto.getUri()).isNull();
            assertThat(deserializedDto.getIp()).isNull();
            assertThat(deserializedDto.getTimestamp()).isNull();
        }
    }

    @Nested
    @DisplayName("Валидация полей")
    class ValidationTests {
        @Test
        @DisplayName("Валидация пустого app")
        void testEmptyAppValidation() {
            // Создаем DTO с нарушением валидационных ограничений
            EndpointHitDto dto = new EndpointHitDto(
                    "", // пустое app (нарушает @NotBlank)
                    "/uri",
                    "192.168.1.1",
                    LocalDateTime.now()
            );

            // Проверяем, что валидация выявила ошибки
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Должно быть обнаружено нарушение валидации для поля app")
                    .isNotEmpty();

            assertThat(violations)
                    .as("Должно быть сообщение об ошибке для поля app")
                    .anyMatch(v -> v.getPropertyPath().toString().equals("app"));
        }

        @Test
        @DisplayName("Валидация длинного uri (более 128 символов)")
        void testUriTooLongValidation() {
            // Создаем URI длиной более 128 символов
            String longUri = "/".repeat(129);

            EndpointHitDto dto = new EndpointHitDto(
                    "app",
                    longUri,
                    "192.168.1.1",
                    LocalDateTime.now()
            );

            // Проверяем, что валидация выявила ошибки
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Должно быть обнаружено нарушение валидации для поля uri")
                    .isNotEmpty();

            assertThat(violations)
                    .as("Должно быть сообщение об ошибке для поля uri")
                    .anyMatch(v -> v.getPropertyPath().toString().equals("uri"));
        }

        @Test
        @DisplayName("Валидация IP адреса (слишком короткий)")
        void testIpTooShortValidation() {
            // IP адрес короче 7 символов
            EndpointHitDto dto = new EndpointHitDto(
                    "app",
                    "/uri",
                    "1.1.1", // слишком короткий IP
                    LocalDateTime.now()
            );

            // Проверяем, что валидация выявила ошибки
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Должно быть обнаружено нарушение валидации для поля ip")
                    .isNotEmpty();

            assertThat(violations)
                    .as("Должно быть сообщение об ошибке для поля ip")
                    .anyMatch(v -> v.getPropertyPath().toString().equals("ip"));
        }

        @Test
        @DisplayName("Валидация даты в будущем")
        void testFutureTimestampValidation() {
            // Timestamp в будущем (не соответствует @PastOrPresent)
            LocalDateTime futureTime = LocalDateTime.now().plus(1, ChronoUnit.DAYS);

            EndpointHitDto dto = new EndpointHitDto(
                    "app",
                    "/uri",
                    "192.168.1.1",
                    futureTime
            );

            // Проверяем, что валидация выявила ошибки
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Должно быть обнаружено нарушение валидации для поля timestamp")
                    .isNotEmpty();

            assertThat(violations)
                    .as("Должно быть сообщение об ошибке для поля timestamp")
                    .anyMatch(v -> v.getPropertyPath().toString().equals("timestamp"));
        }

        @ParameterizedTest
        @MethodSource("invalidDtoProvider")
        @DisplayName("Параметризованный тест для различных нарушений валидации")
        void testValidationConstraints(String app, String uri, String ip, LocalDateTime timestamp, String expectedField) {
            // Создаем DTO с указанными параметрами
            EndpointHitDto dto = new EndpointHitDto(app, uri, ip, timestamp);

            // Проверяем валидацию
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Должна быть обнаружена ошибка валидации")
                    .isNotEmpty();

            assertThat(violations)
                    .as("Должна быть ошибка для поля " + expectedField)
                    .anyMatch(v -> v.getPropertyPath().toString().equals(expectedField));
        }

        static Stream<Arguments> invalidDtoProvider() {
            return Stream.of(
                    // app, uri, ip, timestamp, expectedViolationField
                    Arguments.of(null, "/uri", "192.168.1.1", LocalDateTime.now(), "app"),
                    Arguments.of("app", null, "192.168.1.1", LocalDateTime.now(), "uri"),
                    Arguments.of("app", "/uri", null, LocalDateTime.now(), "ip"),
                    Arguments.of("app", "/uri", "192.168.1.1", null, "timestamp"),
                    Arguments.of("app", "", "192.168.1.1", LocalDateTime.now(), "uri"),
                    Arguments.of("app", "/uri", "ip", LocalDateTime.now(), "ip") // слишком короткий IP
            );
        }
    }

    @Nested
    @DisplayName("Граничные значения")
    class BoundaryTests {
        @Test
        @DisplayName("Максимально допустимая длина app (32 символа)")
        void testMaxAppLength() throws Exception {
            // Создаем app длиной ровно 32 символа
            String maxLengthApp = "a".repeat(32);

            EndpointHitDto dto = new EndpointHitDto(
                    maxLengthApp,
                    "/uri",
                    "192.168.1.1",
                    LocalDateTime.now()
            );

            // Валидация должна пройти успешно
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Не должно быть нарушений валидации для app длиной 32 символа")
                    .isEmpty();

            // Проверяем сериализацию/десериализацию
            String json = objectMapper.writeValueAsString(dto);
            EndpointHitDto deserializedDto = objectMapper.readValue(json, EndpointHitDto.class);

            assertThat(deserializedDto.getApp())
                    .as("App должен быть корректно сериализован и десериализован")
                    .isEqualTo(maxLengthApp);
        }

        @Test
        @DisplayName("Максимально допустимая длина uri (128 символов)")
        void testMaxUriLength() throws Exception {
            // Создаем uri длиной ровно 128 символов
            String maxLengthUri = "/".repeat(128);

            EndpointHitDto dto = new EndpointHitDto(
                    "app",
                    maxLengthUri,
                    "192.168.1.1",
                    LocalDateTime.now()
            );

            // Валидация должна пройти успешно
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Не должно быть нарушений валидации для uri длиной 128 символов")
                    .isEmpty();

            // Проверяем сериализацию/десериализацию
            String json = objectMapper.writeValueAsString(dto);
            EndpointHitDto deserializedDto = objectMapper.readValue(json, EndpointHitDto.class);

            assertThat(deserializedDto.getUri())
                    .as("URI должен быть корректно сериализован и десериализован")
                    .isEqualTo(maxLengthUri);
        }

        @Test
        @DisplayName("Минимально допустимая длина ip (7 символов)")
        void testMinIpLength() throws Exception {
            // Создаем ip длиной ровно 7 символов
            String minLengthIp = "1.1.1.1"; // ровно 7 символов

            EndpointHitDto dto = new EndpointHitDto(
                    "app",
                    "/uri",
                    minLengthIp,
                    LocalDateTime.now()
            );

            // Валидация должна пройти успешно
            Set<ConstraintViolation<EndpointHitDto>> violations = validator.validate(dto);

            assertThat(violations)
                    .as("Не должно быть нарушений валидации для ip длиной 7 символов")
                    .isEmpty();

            // Проверяем сериализацию/десериализацию
            String json = objectMapper.writeValueAsString(dto);
            EndpointHitDto deserializedDto = objectMapper.readValue(json, EndpointHitDto.class);

            assertThat(deserializedDto.getIp())
                    .as("IP должен быть корректно сериализован и десериализован")
                    .isEqualTo(minLengthIp);
        }
    }
}