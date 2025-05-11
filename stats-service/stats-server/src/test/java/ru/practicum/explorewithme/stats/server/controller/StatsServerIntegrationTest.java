package ru.practicum.explorewithme.stats.server.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;
import ru.practicum.explorewithme.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class StatsServerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>("postgres:16.1")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeAll
    static void setUpContainer() {
        postgresqlContainer.start();
    }

    @Test
    void shouldRetrieveUniqueStats_whenUniqueFlagIsTrue() {
        EndpointHitDto hit1 = EndpointHitDto.builder()
                .app("app1")
                .uri("/event/1")
                .ip("192.168.0.1")
                .timestamp(now.minusHours(1))
                .build();

        EndpointHitDto hit2 = EndpointHitDto.builder()
                .app("app1")
                .uri("/event/1")
                .ip("192.168.0.1") // Повторный IP
                .timestamp(now.minusMinutes(30))
                .build();

        ResponseEntity<Void> response1 = restTemplate.postForEntity("/hit", hit1, Void.class);
        ResponseEntity<Void> response2 = restTemplate.postForEntity("/hit", hit2, Void.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String start = now.minusHours(2).format(FORMATTER);
        String end = now.plusHours(1).format(FORMATTER);
        String uris = "/event/1";
        String url = "/stats?start={start}&end={end}&uris={uris}&unique=true";

        ResponseEntity<ViewStatsDto[]> statsResponse = restTemplate.getForEntity(url, ViewStatsDto[].class, start, end, uris);

        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ViewStatsDto[] stats = statsResponse.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats).hasSize(1);

        ViewStatsDto statsEvent1 = stats[0];
        assertThat(statsEvent1.getApp()).isEqualTo("app1");
        assertThat(statsEvent1.getUri()).isEqualTo("/event/1");
        assertThat(statsEvent1.getHits()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmptyStats_whenTimeRangeHasNoHits() {
        String start = now.minusHours(10).format(FORMATTER);
        String end = now.minusHours(8).format(FORMATTER);
        String url = "/stats?start={start}&end={end}";

        ResponseEntity<ViewStatsDto[]> statsResponse = restTemplate.getForEntity(
                url, ViewStatsDto[].class, start, end);

        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ViewStatsDto[] stats = statsResponse.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats).isEmpty();
    }

    @Test
    void shouldReturnStatsForAllUris_whenUrisParameterIsNotProvided() {
        EndpointHitDto hit1 = EndpointHitDto.builder()
                .app("app1")
                .uri("/event/1")
                .ip("192.168.0.1")
                .timestamp(now.minusHours(1))
                .build();

        EndpointHitDto hit2 = EndpointHitDto.builder()
                .app("app2")
                .uri("/event/2")
                .ip("192.168.0.2")
                .timestamp(now.minusMinutes(30))
                .build();

        restTemplate.postForEntity("/hit", hit1, Void.class);
        restTemplate.postForEntity("/hit", hit2, Void.class);

        String start = now.minusHours(2).format(FORMATTER);
        String end = now.plusHours(1).format(FORMATTER);
        String url = "/stats?start={start}&end={end}&unique=false";

        ResponseEntity<ViewStatsDto[]> statsResponse = restTemplate.getForEntity(
                url, ViewStatsDto[].class, start, end);

        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ViewStatsDto[] stats = statsResponse.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats).hasSize(2);
    }

    @Test
    void shouldReturnBadRequest_whenStartIsAfterEnd() {
        String start = now.plusHours(1).format(FORMATTER);
        String end = now.minusHours(1).format(FORMATTER);
        String url = "/stats?start={start}&end={end}";

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Start date cannot be after end date");
    }

    @Test
    void shouldReturnBadRequest_whenHitDtoIsInvalid() {
        EndpointHitDto invalidHit = EndpointHitDto.builder()
                .uri("/event/1")
                .ip("192.168.0.1")
                .timestamp(now.minusHours(1))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/hit", invalidHit, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Validation error");
    }
}