package ru.practicum.explorewithme.main.controller.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.service.EventService;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

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

    @Test
    @DisplayName("Поиск событий администратором: должен вернуть 200 OK и пустой список, если "
        + "событий не найдено")
    void searchEventsAdmin_whenNoEventsFound_shouldReturnOkAndEmptyList() throws Exception {
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
    @DisplayName("Поиск событий администратором: должен вернуть 200 OK и список событий, если они"
        + " найдены")
    void searchEventsAdmin_whenEventsFound_shouldReturnOkAndEventList() throws Exception {
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
    @DisplayName("Поиск событий администратором: должен корректно передавать все параметры "
        + "фильтрации в сервис")
    void searchEventsAdmin_withAllFilters_shouldPassFiltersToService() throws Exception {
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
    @DisplayName("Поиск событий администратором: должен использовать значения по умолчанию для "
        + "from и size, если они не переданы")
    void searchEventsAdmin_withDefaultPagination_shouldUseDefaultValues() throws Exception {
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
    @DisplayName("Поиск событий администратором: должен вернуть 400 Bad Request при невалидном "
        + "значении 'from'")
    void searchEventsAdmin_withInvalidFrom_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events").param("from", "-1") // Невалидное значение
                .param("size", "10").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(eventService); // Сервис не должен вызываться
    }

    @Test
    @DisplayName("Поиск событий администратором: должен вернуть 400 Bad Request при невалидном "
        + "значении 'size'")
    void searchEventsAdmin_withInvalidSize_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events").param("from", "0")
            .param("size", "0") // Невалидное значение (@Positive)
            .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("Поиск событий администратором: должен вернуть 400 Bad Request при некорректном "
        + "формате rangeStart")
    void searchEventsAdmin_withInvalidRangeStartFormat_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/events").param("rangeStart", "invalid-date-format")
            .param("rangeEnd", LocalDateTime.now().format(formatter)).param("from", "0")
            .param("size", "10")).andExpect(status().isBadRequest());
        verifyNoInteractions(eventService);
    }
}