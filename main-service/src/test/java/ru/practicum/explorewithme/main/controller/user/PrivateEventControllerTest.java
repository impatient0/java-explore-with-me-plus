package ru.practicum.explorewithme.main.controller.user;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.service.EventService;

@WebMvcTest(PrivateEventController.class)
@DisplayName("Тесты для PrivateEventController")
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private final Long testUserId = 1L;

    @Nested
    @DisplayName("GET /users/{userId}/events:")
    class GetUserEventsTest {
        @Test
        @DisplayName("должен вернуть 200 OK и пустой список, если событий не найдено")
        void getEventsAddedByCurrentUser_whenNoEventsFound_shouldReturnOkAndEmptyList() throws Exception {
            when(eventService.getEventsByOwner(eq(testUserId), eq(0), eq(10)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .param("from", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

            verify(eventService).getEventsByOwner(testUserId, 0, 10);
        }

        @Test
        @DisplayName("должен вернуть 200 OK и список событий, если они найдены")
        void getEventsAddedByCurrentUser_whenEventsFound_shouldReturnOkAndEventList() throws Exception {
            EventShortDto eventDto1 = EventShortDto.builder().id(1L).title("Event 1").eventDate(LocalDateTime.now()).build();
            EventShortDto eventDto2 = EventShortDto.builder().id(2L).title("Event 2").eventDate(LocalDateTime.now()).build();
            List<EventShortDto> events = List.of(eventDto1, eventDto2);

            when(eventService.getEventsByOwner(eq(testUserId), eq(0), eq(20)))
                .thenReturn(events);

            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .param("from", "0")
                    .param("size", "20")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(eventDto1.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is(eventDto1.getTitle())))
                .andExpect(jsonPath("$[1].id", is(eventDto2.getId().intValue())))
                .andExpect(jsonPath("$[1].title", is(eventDto2.getTitle())));

            verify(eventService).getEventsByOwner(testUserId, 0, 20);
        }

        @Test
        @DisplayName("должен использовать значения по умолчанию для from и size")
        void getEventsAddedByCurrentUser_withDefaultPagination_shouldUseDefaultValues() throws Exception {
            when(eventService.getEventsByOwner(eq(testUserId), eq(0), eq(10))) // Ожидаем дефолтные 0 и 10
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    // from и size не передаются
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(eventService).getEventsByOwner(testUserId, 0, 10);
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном from")
        void getEventsAddedByCurrentUser_withInvalidFrom_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .param("from", "-1") // Невалидное значение @PositiveOrZero
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService); // Валидация происходит на уровне контроллера
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном size")
        void getEventsAddedByCurrentUser_withInvalidSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .param("from", "0")
                    .param("size", "0") // Невалидное значение @Positive
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService); // Валидация происходит на уровне контроллера
        }
    }
}