package ru.practicum.explorewithme.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestClient;
import ru.practicum.explorewithme.stats.client.StatsClient;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.explorewithme.main", "ru.practicum.explorewithme.stats.client"})
public class MainServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MainServiceApplication.class, args);
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder, @Value("${stats-server.url}") String serverUrl) {
        return builder
                .baseUrl(serverUrl)
                .build();
    }

    @Bean
    public CommandLineRunner commandLineRunner(StatsClient statsClient) {
        return args -> {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            statsClient.saveHit(new EndpointHitDto(
                    "ewm-main-app",
                    "/events/1",
                    "192.168.1.1",
                    LocalDateTime.parse("2025-01-01 00:00:01",formatter)
            ));
            statsClient.saveHit(new EndpointHitDto(
                    "ewm-test-app",
                    "/events/3",
                    "192.168.1.1",
                    LocalDateTime.parse("2025-01-02 00:00:03",formatter)
            ));
            statsClient.saveHit(new EndpointHitDto(
                    "ewm-main-app",
                    "/events/2",
                    "192.168.1.2",
                    LocalDateTime.parse("2025-01-03 00:00:02",formatter)
            ));
            statsClient.saveHit(new EndpointHitDto(
                    "ewm-app",
                    "/events/4",
                    "192.168.1.4",
                    LocalDateTime.parse("2025-01-04 00:00:04",formatter)
            ));
            System.out.println(statsClient.getStats("2025-01-01 00:00:00", "2025-01-06 00:00:00", null, true));
            System.out.println("\n");
            System.out.println(statsClient.getStats("2025-01-01 00:00:00", "2025-01-03 00:00:00", null, false));
            System.out.println("\n");
            System.out.println(statsClient.getStats("2025-01-02 00:00:00", "2025-01-05 00:00:00", null, false));
        };
    }
}

