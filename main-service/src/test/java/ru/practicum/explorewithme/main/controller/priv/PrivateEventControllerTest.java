package ru.practicum.explorewithme.main.controller.priv;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMATTER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.dto.UpdateEventUserRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.service.EventService;

@WebMvcTest(PrivateEventController.class)
@DisplayName("Тесты для PrivateEventController")
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Может пригодиться в дальнейших тестах

    @MockitoBean
    private EventService eventService;

    private final Long testUserId = 1L;
    private final DateTimeFormatter formatter = DATE_TIME_FORMATTER;


    @Nested
    @DisplayName("GET /users/{userId}/events: Получение списка событий пользователя")
    class GetUserEventsListTest {
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
            LocalDateTime now = LocalDateTime.now().withNano(0);
            EventShortDto eventDto1 = EventShortDto.builder().id(1L).title("Event 1").eventDate(now).build();
            EventShortDto eventDto2 = EventShortDto.builder().id(2L).title("Event 2").eventDate(now).build();
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
                .andExpect(jsonPath("$[0].eventDate", is(now.format(formatter))))
                .andExpect(jsonPath("$[1].id", is(eventDto2.getId().intValue())))
                .andExpect(jsonPath("$[1].title", is(eventDto2.getTitle())));

            verify(eventService).getEventsByOwner(testUserId, 0, 20);
        }

        @Test
        @DisplayName("должен использовать значения по умолчанию для from и size")
        void getEventsAddedByCurrentUser_withDefaultPagination_shouldUseDefaultValues() throws Exception {
            when(eventService.getEventsByOwner(eq(testUserId), eq(0), eq(10)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(eventService).getEventsByOwner(testUserId, 0, 10);
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном from")
        void getEventsAddedByCurrentUser_withInvalidFrom_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}/events", testUserId)
                    .param("from", "-1")
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
                    .param("size", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService); // Валидация происходит на уровне контроллера
        }
    }

    @Nested
    @DisplayName("GET /users/{userId}/events/{eventId}: Получение полной информации о событии пользователя")
    class GetFullEventInfoByOwnerTest {

        private final Long testEventId = 100L;

        @Test
        @DisplayName("должен вернуть 200 OK и EventFullDto, если событие найдено и принадлежит пользователю")
        void getFullEventInfoByOwner_whenEventFound_shouldReturnOkAndEventFullDto() throws Exception {
            LocalDateTime now = LocalDateTime.now().withNano(0);
            EventFullDto expectedDto = EventFullDto.builder()
                .id(testEventId)
                .title("Full Event Title")
                .annotation("Full Annotation")
                .description("Full Description")
                .eventDate(now.plusDays(1))
                .createdOn(now.minusHours(5))
                .paid(true)
                .participantLimit(50)
                .build();

            when(eventService.getEventPrivate(eq(testUserId), eq(testEventId))).thenReturn(expectedDto);

            mockMvc.perform(get("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(expectedDto.getId().intValue())))
                .andExpect(jsonPath("$.title", is(expectedDto.getTitle())))
                .andExpect(jsonPath("$.annotation", is(expectedDto.getAnnotation())))
                .andExpect(jsonPath("$.eventDate", is(expectedDto.getEventDate().format(formatter))));

            verify(eventService).getEventPrivate(testUserId, testEventId);
        }

        @Test
        @DisplayName("должен вернуть 404 Not Found, если событие не найдено или не принадлежит пользователю")
        void getFullEventInfoByOwner_whenEventNotFound_shouldReturnNotFound() throws Exception {
            String errorMessage = String.format("Event with id=%d and initiatorId=%d not found", testEventId, testUserId);
            when(eventService.getEventPrivate(eq(testUserId), eq(testEventId)))
                .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(get("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason", is("Requested object not found")))
                .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(eventService).getEventPrivate(testUserId, testEventId);
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном userId в пути")
        void getFullEventInfoByOwner_withInvalidUserIdPath_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}/events/{eventId}", "invalidUserId", testEventId))
                .andExpect(status().isBadRequest()); // Ошибка преобразования типа для @PathVariable Long userId
            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном eventId в пути")
        void getFullEventInfoByOwner_withInvalidEventIdPath_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/users/{userId}/events/{eventId}", testUserId, "invalidEventId"))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
        }
    }

    @Nested
    @DisplayName("PATCH /users/{userId}/events/{eventId}: Изменение события пользователем")
    class UpdateEventByOwnerTest {

        private final Long testEventId = 200L;

        private UpdateEventUserRequestDto createValidUpdateDto() {
            return UpdateEventUserRequestDto.builder()
                .title("Updated Event Title")
                .annotation("Valid Updated Annotation")
                .eventDate(LocalDateTime.now().plusHours(3).withNano(0))
                .build();
        }

        @Test
        @DisplayName("должен вернуть 200 OK и обновленный EventFullDto при успешном обновлении")
        void updateEventByOwner_whenUpdateSuccessful_shouldReturnOkAndUpdatedDto() throws Exception {
            UpdateEventUserRequestDto updateDto = createValidUpdateDto();
            EventFullDto updatedEventFullDto = EventFullDto.builder()
                .id(testEventId)
                .title(updateDto.getTitle())
                .annotation(updateDto.getAnnotation())
                .eventDate(updateDto.getEventDate())
                .build();

            when(eventService.updateEventByOwner(eq(testUserId), eq(testEventId), any(UpdateEventUserRequestDto.class)))
                .thenReturn(updatedEventFullDto);

            mockMvc.perform(patch("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testEventId.intValue())))
                .andExpect(jsonPath("$.title", is(updateDto.getTitle())))
                .andExpect(jsonPath("$.annotation", is(updateDto.getAnnotation())))
                .andExpect(jsonPath("$.eventDate", is(updateDto.getEventDate().format(formatter))));

            verify(eventService).updateEventByOwner(testUserId, testEventId, updateDto);
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request, если DTO обновления невалиден (например, короткое название)")
        void updateEventByOwner_whenDtoIsInvalid_shouldReturnBadRequest() throws Exception {
            UpdateEventUserRequestDto invalidUpdateDto = UpdateEventUserRequestDto.builder()
                .title("S")
                .build();

            mockMvc.perform(patch("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUpdateDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("должен вернуть 404 Not Found, если событие не найдено или не принадлежит пользователю")
        void updateEventByOwner_whenEventNotFound_shouldReturnNotFound() throws Exception {
            UpdateEventUserRequestDto updateDto = createValidUpdateDto();
            String errorMessage = "Event or user not found";
            when(eventService.updateEventByOwner(eq(testUserId), eq(testEventId), any(UpdateEventUserRequestDto.class)))
                .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(patch("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(eventService).updateEventByOwner(testUserId, testEventId, updateDto);
        }

        @Test
        @DisplayName("должен вернуть 409 Conflict, если событие нельзя обновить (например, уже опубликовано)")
        void updateEventByOwner_whenUpdateNotAllowed_shouldReturnConflict() throws Exception {
            UpdateEventUserRequestDto updateDto = createValidUpdateDto();
            String errorMessage = "Cannot update published event";
            when(eventService.updateEventByOwner(eq(testUserId), eq(testEventId), any(UpdateEventUserRequestDto.class)))
                .thenThrow(new BusinessRuleViolationException(errorMessage));

            mockMvc.perform(patch("/users/{userId}/events/{eventId}", testUserId, testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(eventService).updateEventByOwner(testUserId, testEventId, updateDto);
        }
    }
}