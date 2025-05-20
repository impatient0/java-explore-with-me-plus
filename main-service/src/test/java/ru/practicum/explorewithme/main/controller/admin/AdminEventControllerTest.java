package ru.practicum.explorewithme.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.UpdateEventAdminRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.service.EventService;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMATTER;

@WebMvcTest(AdminEventController.class)
@DisplayName("Тесты для AdminEventController")
class AdminEventControllerTest {

    private final DateTimeFormatter formatter = DATE_TIME_FORMATTER;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EventService eventService;

    @Nested
    @DisplayName("GET /admin/events: Поиск событий администратором")
    class GetEventsByAdminTests {

        @Test
        @DisplayName("Должен вернуть 200 OK и пустой список, если событий не найдено")
        void whenNoEventsFound_shouldReturnOkAndEmptyList() throws Exception {
            when(eventService.getEventsAdmin(any(AdminEventSearchParams.class), anyInt(),
                anyInt())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/events").param("from", "0").param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

            AdminEventSearchParams expectedParams = AdminEventSearchParams.builder()
                .users(null)
                .states(null)
                .categories(null)
                .rangeStart(null)
                .rangeEnd(null)
                .build();
            verify(eventService).getEventsAdmin(eq(expectedParams), eq(0), eq(10));
        }

        @Test
        @DisplayName("Должен вернуть 200 OK и список событий, если они найдены")
        void whenEventsFound_shouldReturnOkAndEventList() throws Exception {
            LocalDateTime eventTime = LocalDateTime.now().plusDays(5).withNano(0);
            EventFullDto eventDto = EventFullDto.builder().id(1L).title("Test Event")
                .annotation("Test Annotation").eventDate(eventTime).build();
            List<EventFullDto> events = List.of(eventDto);

            when(eventService.getEventsAdmin(any(AdminEventSearchParams.class), eq(0),
                eq(10))).thenReturn(events);

            mockMvc.perform(get("/admin/events").param("from", "0").param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(eventDto.getId().intValue())))
                .andExpect(jsonPath("$[0].title", is(eventDto.getTitle()))).andExpect(
                    jsonPath("$[0].eventDate", is(eventTime.format(formatter))));

            AdminEventSearchParams expectedParams = AdminEventSearchParams.builder()
                .users(null)
                .states(null)
                .categories(null)
                .rangeStart(null)
                .rangeEnd(null)
                .build();
            verify(eventService).getEventsAdmin(eq(expectedParams), eq(0), eq(10));
        }

        @Test
        @DisplayName("Должен корректно передавать все параметры фильтрации в сервис")
        void withAllFilters_shouldPassFiltersToService() throws Exception {
            List<Long> userIds = List.of(1L, 2L);
            List<EventState> states = List.of(EventState.PENDING, EventState.PUBLISHED);
            List<Long> categoryIds = List.of(10L, 20L);
            LocalDateTime rangeStart = LocalDateTime.now().minusDays(1).withNano(0);
            LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1).withNano(0);
            int from = 5;
            int size = 15;

            AdminEventSearchParams expectedSearchParams = AdminEventSearchParams.builder()
                .users(userIds)
                .states(states)
                .categories(categoryIds)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();

            when(eventService.getEventsAdmin(eq(expectedSearchParams), eq(from), eq(size)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(
                    get("/admin/events").param("users", "1", "2").param("states", "PENDING",
                            "PUBLISHED")
                        .param("categories", "10", "20").param("rangeStart",
                            rangeStart.format(formatter))
                        .param("rangeEnd", rangeEnd.format(formatter)).param("from",
                            String.valueOf(from))
                        .param("size", String.valueOf(size)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

            verify(eventService).getEventsAdmin(eq(expectedSearchParams), eq(from), eq(size));
        }

        @Test
        @DisplayName("Должен использовать значения по умолчанию для from и size, если они не переданы")
        void withDefaultPagination_shouldUseDefaultValues() throws Exception {
            when(eventService.getEventsAdmin(any(AdminEventSearchParams.class), eq(0),
                eq(10))).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/events").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            AdminEventSearchParams expectedParams = AdminEventSearchParams.builder()
                .users(null)
                .states(null)
                .categories(null)
                .rangeStart(null)
                .rangeEnd(null)
                .build();
            verify(eventService).getEventsAdmin(eq(expectedParams), eq(0), eq(10));
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном значении 'from'")
        void withInvalidFrom_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/events").param("from", "-1") // Невалидное значение
                    .param("size", "10").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService); // Сервис не должен вызываться
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном значении 'size'")
        void withInvalidSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/events").param("from", "0")
                .param("size", "0") // Невалидное значение (@Positive)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при некорректном формате rangeStart")
        void withInvalidRangeStartFormat_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/events").param("rangeStart", "invalid-date-format")
                .param("rangeEnd", LocalDateTime.now().format(formatter)).param("from", "0")
                .param("size", "10")).andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
        }
    }


    @Nested
    @DisplayName("PATCH /admin/events/{eventId}: Модерация события администратором")
    class ModerateEventByAdminTests {

        private final Long testEventId = 1L;
        private UpdateEventAdminRequestDto validPublishRequestDto;
        private UpdateEventAdminRequestDto validRejectRequestDto;
        private EventFullDto moderatedEventFullDto;

        @BeforeEach
        void setUpModerateTests() {
            validPublishRequestDto = UpdateEventAdminRequestDto.builder()
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .title("Published by Admin")
                .build();

            validRejectRequestDto = UpdateEventAdminRequestDto.builder()
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.REJECT_EVENT)
                .build();

            moderatedEventFullDto = EventFullDto.builder()
                .id(testEventId)
                .title("Moderated Event")
                .state(EventState.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(2).withNano(0))
                .build();
        }

        @Test
        @DisplayName("Должен вернуть 200 OK и обновленный EventFullDto при успешной публикации")
        void whenPublishSuccessful_shouldReturnOkAndDto() throws Exception {
            when(eventService.moderateEventByAdmin(eq(testEventId), any(UpdateEventAdminRequestDto.class)))
                .thenReturn(moderatedEventFullDto);

            mockMvc.perform(patch("/admin/events/{eventId}", testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPublishRequestDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testEventId.intValue())))
                .andExpect(jsonPath("$.title", is(moderatedEventFullDto.getTitle())))
                .andExpect(jsonPath("$.state", is(EventState.PUBLISHED.toString())));

            verify(eventService).moderateEventByAdmin(eq(testEventId), eq(validPublishRequestDto));
        }

        @Test
        @DisplayName("Должен вернуть 200 OK и обновленный EventFullDto при успешном отклонении")
        void whenRejectSuccessful_shouldReturnOkAndDto() throws Exception {
            moderatedEventFullDto.setState(EventState.CANCELED);
            when(eventService.moderateEventByAdmin(eq(testEventId), any(UpdateEventAdminRequestDto.class)))
                .thenReturn(moderatedEventFullDto);

            mockMvc.perform(patch("/admin/events/{eventId}", testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRejectRequestDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(testEventId.intValue())))
                .andExpect(jsonPath("$.state", is(EventState.CANCELED.toString())));

            verify(eventService).moderateEventByAdmin(eq(testEventId), eq(validRejectRequestDto));
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request, если DTO обновления невалиден (например, слишком короткий title)")
        void whenDtoIsInvalid_shouldReturnBadRequest() throws Exception {
            UpdateEventAdminRequestDto invalidDto = UpdateEventAdminRequestDto.builder()
                .title("S")
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .build();

            mockMvc.perform(patch("/admin/events/{eventId}", testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(eventService);
        }

        @Test
        @DisplayName("Должен вернуть 404 Not Found, если событие для модерации не найдено")
        void whenEventNotFound_shouldReturnNotFound() throws Exception {
            String errorMessage = "Event with id=" + testEventId + " not found.";
            when(eventService.moderateEventByAdmin(eq(testEventId), any(UpdateEventAdminRequestDto.class)))
                .thenThrow(new EntityNotFoundException(errorMessage));

            mockMvc.perform(patch("/admin/events/{eventId}", testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPublishRequestDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason", is("Requested object not found")))
                .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(eventService).moderateEventByAdmin(eq(testEventId), eq(validPublishRequestDto));
        }

        @Test
        @DisplayName("Должен вернуть 409 Conflict, если событие нельзя модерировать (например, неверное состояние)")
        void whenModerationNotAllowed_shouldReturnConflict() throws Exception {
            String errorMessage = "Cannot publish the event because it's not in the PENDING state.";
            when(eventService.moderateEventByAdmin(eq(testEventId), any(UpdateEventAdminRequestDto.class)))
                .thenThrow(new BusinessRuleViolationException(errorMessage));

            mockMvc.perform(patch("/admin/events/{eventId}", testEventId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validPublishRequestDto))
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason", is("Conditions not met for requested operation")))
                .andExpect(jsonPath("$.message", is(errorMessage)));

            verify(eventService).moderateEventByAdmin(eq(testEventId), eq(validPublishRequestDto));
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном eventId в пути")
        void withInvalidEventIdPath_shouldReturnBadRequest() throws Exception {
            UpdateEventAdminRequestDto dto = UpdateEventAdminRequestDto.builder()
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT).build();

            mockMvc.perform(patch("/admin/events/{eventId}", "invalidEventId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
        }
    }
}