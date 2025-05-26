package ru.practicum.explorewithme.main.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.aspect.StatsHitAspect;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.service.EventService;
import ru.practicum.explorewithme.main.service.params.PublicEventSearchParams;
import ru.practicum.explorewithme.stats.client.StatsClient;
import ru.practicum.explorewithme.stats.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMATTER;


@WebMvcTest(PublicEventController.class)
@Import({StatsHitAspect.class})
@EnableAspectJAutoProxy
@TestPropertySource(properties = {"spring.application.name=test-main-service-for-aspect"})
@DisplayName("Тесты для PublicEventController и срабатывания StatsHitAspect")
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private StatsClient statsClient;

    @Value("${spring.application.name}")
    private String configuredAppName;

    @Captor
    private ArgumentCaptor<EndpointHitDto> hitDtoCaptor;

    private final DateTimeFormatter formatter = DATE_TIME_FORMATTER;
    private final String testIpAddress = "192.168.0.1";


    @BeforeEach
    void setUp() {
    }

    @Nested
    @DisplayName("GET /events")
    class EventsEndpointTests {

        @Test
        @DisplayName("должен успешно возвращать список событий и отправлять хит в статистику")
        void shouldReturnEventsAndLogHit() throws Exception {
            EventShortDto event1 = EventShortDto.builder().id(1L).title("Event Alpha").build();
            List<EventShortDto> mockEvents = List.of(event1);

            when(eventService.getEventsPublic(any(PublicEventSearchParams.class), anyInt(), anyInt()))
                .thenReturn(mockEvents);

            mockMvc.perform(get("/events")
                    .param("text", "search text")
                    .param("from", "0")
                    .param("size", "10")
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Event Alpha")));

            PublicEventSearchParams expectedSearchParams = PublicEventSearchParams.builder()
                .text("search text")
                .categories(null)
                .paid(null)
                .rangeStart(null)
                .rangeEnd(null)
                .onlyAvailable(false)
                .sort("EVENT_DATE")
                .build();
            verify(eventService).getEventsPublic(eq(expectedSearchParams), eq(0), eq(10));

            verify(statsClient, times(1)).saveHit(hitDtoCaptor.capture());
            EndpointHitDto capturedHit = hitDtoCaptor.getValue();
            assertEquals(configuredAppName, capturedHit.getApp());
            assertEquals("/events", capturedHit.getUri());
            assertEquals(testIpAddress, capturedHit.getIp());
            assertNotNull(capturedHit.getTimestamp());
        }

        @Test
        @DisplayName("должен отправлять хит даже если сервис событий вернул пустой список")
        void whenServiceReturnsEmpty_shouldStillLogHit() throws Exception {
            when(eventService.getEventsPublic(any(PublicEventSearchParams.class), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events")
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

            verify(statsClient, times(1)).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен использовать IP из заголовка X-Real-IP, если он есть")
        void withXRealIpHeader_shouldUseHeaderIpForHit() throws Exception {
            when(eventService.getEventsPublic(any(), anyInt(), anyInt())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events")
                    .header("X-Real-IP", "10.0.0.1")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(statsClient).saveHit(hitDtoCaptor.capture());
            assertEquals("10.0.0.1", hitDtoCaptor.getValue().getIp());
        }

        @Test
        @DisplayName("должен использовать IP из request.getRemoteAddr(), если заголовок X-Real-IP отсутствует")
        void withoutXRealIpHeader_shouldUseRemoteAddrForHit() throws Exception {
            String defaultMockIp = "127.0.0.1";
            when(eventService.getEventsPublic(any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(statsClient).saveHit(hitDtoCaptor.capture());
            assertEquals(defaultMockIp, hitDtoCaptor.getValue().getIp());
        }

        @Test
        @DisplayName("должен корректно передавать все параметры фильтрации в сервис")
        void withAllFilters_shouldPassAllParamsToService() throws Exception {
            String text = "party";
            List<Long> categories = Arrays.asList(1L, 2L);
            Boolean paid = true;
            LocalDateTime rangeStart = LocalDateTime.now().plusDays(1).withNano(0);
            LocalDateTime rangeEnd = LocalDateTime.now().plusDays(2).withNano(0);
            boolean onlyAvailable = true;
            String sort = "VIEWS";
            int from = 5;
            int size = 15;
            String ip = "10.0.0.2";

            PublicEventSearchParams expectedSearchParams = PublicEventSearchParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .build();

            when(eventService.getEventsPublic(eq(expectedSearchParams), eq(from), eq(size)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events")
                    .param("text", text)
                    .param("categories", "1", "2")
                    .param("paid", paid.toString())
                    .param("rangeStart", rangeStart.format(formatter))
                    .param("rangeEnd", rangeEnd.format(formatter))
                    .param("onlyAvailable", String.valueOf(onlyAvailable))
                    .param("sort", sort)
                    .param("from", String.valueOf(from))
                    .param("size", String.valueOf(size))
                    .header("X-Real-IP", ip)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(eventService).getEventsPublic(eq(expectedSearchParams), eq(from), eq(size));
            verify(statsClient).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен использовать значения по умолчанию для onlyAvailable и sort, если они не переданы")
        void withDefaultSortAndAvailability_shouldUseDefaultValuesInServiceCall() throws Exception {
            int from = 0;
            int size = 10;
            String defaultMockIp = "127.0.0.1";


            PublicEventSearchParams expectedSearchParams = PublicEventSearchParams.builder()
                .text(null)
                .categories(null)
                .paid(null)
                .rangeStart(null)
                .rangeEnd(null)
                .onlyAvailable(false)
                .sort("EVENT_DATE")
                .build();

            when(eventService.getEventsPublic(eq(expectedSearchParams), eq(from), eq(size)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events")
                    .param("from", String.valueOf(from))
                    .param("size", String.valueOf(size))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(eventService).getEventsPublic(eq(expectedSearchParams), eq(from), eq(size));
            verify(statsClient).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном значении 'from'")
        void withInvalidFrom_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/events")
                    .param("from", "-1")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
            verify(statsClient, never()).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном значении 'size'")
        void withInvalidSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/events")
                    .param("from", "0")
                    .param("size", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(eventService);
            verify(statsClient, never()).saveHit(any(EndpointHitDto.class));
        }
    }

    @Nested
    @DisplayName("GET /events/{eventId}")
    class EventsByIdEndpointTests {

        @Test
        @DisplayName("должен успешно возвращать событие и отправлять хит в статистику")
        void shouldReturnEventAndLogHit() throws Exception {
            Long eventId = 1L;
            EventFullDto mockEvent = EventFullDto.builder()
                .id(eventId)
                .title("Specific Event")
                .eventDate(LocalDateTime.now().plusDays(1).withNano(0))
                .build();

            when(eventService.getEventByIdPublic(eq(eventId))).thenReturn(mockEvent);

            mockMvc.perform(get("/events/{eventId}", eventId)
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventId.intValue())))
                .andExpect(jsonPath("$.title", is("Specific Event")));

            verify(eventService).getEventByIdPublic(eq(eventId));

            verify(statsClient, times(1)).saveHit(hitDtoCaptor.capture());
            EndpointHitDto capturedHit = hitDtoCaptor.getValue();
            assertEquals(configuredAppName, capturedHit.getApp());
            assertEquals("/events/" + eventId, capturedHit.getUri());
            assertEquals(testIpAddress, capturedHit.getIp());
            assertNotNull(capturedHit.getTimestamp());
        }

        @Test
        @DisplayName("должен отправлять хит даже если сервис событий выбросил NotFoundException")
        void whenServiceThrowsNotFound_shouldStillLogHitAndReturn404() throws Exception {
            Long eventId = 999L;
            when(eventService.getEventByIdPublic(eq(eventId)))
                .thenThrow(new ru.practicum.explorewithme.main.error.EntityNotFoundException("Event not found"));

            mockMvc.perform(get("/events/{eventId}", eventId)
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

            verify(statsClient, never()).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен вернуть 400 Bad Request при невалидном eventId в пути")
        void withInvalidEventIdPath_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/events/{eventId}", "notANumber")
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(eventService);
            verify(statsClient, never()).saveHit(any(EndpointHitDto.class));
        }

        @Test
        @DisplayName("должен проверять полные данные в EventFullDto при успешном ответе")
        void whenEventFound_shouldReturnCorrectEventFullDtoFields() throws Exception {
            Long eventId = 1L;
            LocalDateTime eventDate = LocalDateTime.now().plusDays(1).withNano(0);
            LocalDateTime createdOn = LocalDateTime.now().minusHours(5).withNano(0);
            LocalDateTime publishedOn = LocalDateTime.now().minusHours(1).withNano(0);

            EventFullDto mockEvent = EventFullDto.builder()
                .id(eventId)
                .title("Specific Event Title")
                .annotation("Specific Annotation")
                .description("Specific Description")
                .eventDate(eventDate)
                .createdOn(createdOn)
                .publishedOn(publishedOn)
                .paid(true)
                .participantLimit(100)
                .requestModeration(false)
                .state(EventState.PUBLISHED)
                .views(1000L)
                .confirmedRequests(50L)
                .build();

            when(eventService.getEventByIdPublic(eq(eventId))).thenReturn(mockEvent);

            mockMvc.perform(get("/events/{eventId}", eventId)
                    .header("X-Real-IP", testIpAddress)
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventId.intValue())))
                .andExpect(jsonPath("$.title", is("Specific Event Title")))
                .andExpect(jsonPath("$.annotation", is("Specific Annotation")))
                .andExpect(jsonPath("$.description", is("Specific Description")))
                .andExpect(jsonPath("$.eventDate", is(eventDate.format(formatter))))
                .andExpect(jsonPath("$.createdOn", is(createdOn.format(formatter))))
                .andExpect(jsonPath("$.publishedOn", is(publishedOn.format(formatter))))
                .andExpect(jsonPath("$.paid", is(true)))
                .andExpect(jsonPath("$.participantLimit", is(100)))
                .andExpect(jsonPath("$.requestModeration", is(false)))
                .andExpect(jsonPath("$.state", is("PUBLISHED")))
                .andExpect(jsonPath("$.views", is(1000)))
                .andExpect(jsonPath("$.confirmedRequests", is(50)));

            verify(statsClient, times(1)).saveHit(any(EndpointHitDto.class));
        }
    }
}