package ru.practicum.explorewithme.main.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.querydsl.core.types.Predicate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.models.Event;
import ru.practicum.explorewithme.main.models.EventState;
import ru.practicum.explorewithme.main.models.QEvent;
import ru.practicum.explorewithme.main.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для реализации EventService")
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    @Captor
    private ArgumentCaptor<Predicate> predicateCaptor;

    private LocalDateTime now;
    private LocalDateTime plusOneHour;
    private LocalDateTime plusTwoHours;
    private QEvent qEvent;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        plusOneHour = now.plusHours(1);
        plusTwoHours = now.plusHours(2);
        qEvent = QEvent.event;
    }

    @Nested
    @DisplayName("Метод getEventsAdmin")
    class GetEventsAdminTests {

        @Test
        @DisplayName("Должен формировать предикат, если передан фильтр по пользователям")
        void getEventsAdmin_withUserFilter_shouldApplyUserPredicate() {
            List<Long> users = List.of(1L, 2L);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(users, null, null, null, null, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate, "Предикат не должен быть null, если есть фильтры");

            String predicateString = capturedPredicate.toString();
            assertTrue(predicateString.contains(qEvent.initiator.id.toString())
                    && predicateString.contains("in [1, 2]"),
                "Предикат должен содержать фильтр по ID пользователей");
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Должен формировать предикат, если передан фильтр по состояниям")
        void getEventsAdmin_withStateFilter_shouldApplyStatePredicate() {
            List<EventState> states = List.of(EventState.PENDING, EventState.PUBLISHED);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(null, states, null, null, null, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();
            assertTrue(
                predicateString.contains(qEvent.state.toString()) && predicateString.contains(
                    "in [" + EventState.PENDING + ", " + EventState.PUBLISHED + "]"),
                "Предикат должен содержать фильтр по состояниям");
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Должен формировать предикат, если передан фильтр по категориям")
        void getEventsAdmin_withCategoryFilter_shouldApplyCategoryPredicate() {
            List<Long> categories = List.of(5L);
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(null, null, categories, null, null, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();

            String categoryIdPath = qEvent.category.id.toString();

            assertTrue(predicateString.contains(categoryIdPath + " = " + categories.getFirst()),
                "Предикат должен содержать фильтр по ID категорий");
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }


        @Test
        @DisplayName("Должен формировать предикат, если передана начальная дата диапазона")
        void getEventsAdmin_withRangeStart_shouldApplyRangeStartPredicate() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(null, null, null, now, null, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();
            assertTrue(
                predicateString.contains(qEvent.eventDate.toString()) && predicateString.contains(
                    now.toString()), // goe(now)
                "Предикат должен содержать фильтр по начальной дате");
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Должен формировать предикат, если передана конечная дата диапазона")
        void getEventsAdmin_withRangeEnd_shouldApplyRangeEndPredicate() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(null, null, null, null, plusTwoHours, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();
            assertTrue(
                predicateString.contains(qEvent.eventDate.toString()) && predicateString.contains(
                    plusTwoHours.toString()), // loe(plusTwoHours)
                "Предикат должен содержать фильтр по конечной дате");
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Поиск без фильтров должен вызывать eventRepository.findAll с 'пустым' "
            + "предикатом")
        void getEventsAdmin_whenNoFilters_shouldCallRepositoryWithEmptyPredicate() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(eventRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(emptyPage);

            eventService.getEventsAdmin(null, null, null, null, null, 0, 10);

            ArgumentCaptor<Predicate> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
            verify(eventRepository).findAll(predicateCaptor.capture(), eq(pageable));

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
        }

        @Test
        @DisplayName("Должен корректно формировать предикат со всеми фильтрами одновременно")
        void getEventsAdmin_withAllFilters_shouldApplyAllPredicates() {
            List<Long> users = List.of(1L);
            List<EventState> states = List.of(EventState.PUBLISHED);
            List<Long> categories = List.of(10L);
            LocalDateTime rangeStart = now;
            LocalDateTime rangeEnd = plusTwoHours;
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            eventService.getEventsAdmin(users, states, categories, rangeStart, rangeEnd, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();

            String initiatorIdPath = qEvent.initiator.id.toString();
            String statePath = qEvent.state.toString();
            String categoryIdPath = qEvent.category.id.toString();
            String eventDatePath = qEvent.eventDate.toString();

            assertAll("Проверка всех частей предиката", () -> assertTrue(
                predicateString.contains(initiatorIdPath + " = " + users.getFirst()),
                "Фильтр по пользователям"), () -> assertTrue(
                predicateString.contains(statePath + " = " + states.getFirst().toString()),
                "Фильтр по состояниям"), () -> assertTrue(
                predicateString.contains(categoryIdPath + " = " + categories.getFirst()),
                "Фильтр по категориям"), () -> assertTrue(
                predicateString.contains(eventDatePath + " >= " + rangeStart.toString()),
                "Фильтр по начальной дате"), () -> assertTrue(
                predicateString.contains(eventDatePath + " <= " + rangeEnd.toString()),
                "Фильтр по конечной дате"));
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Должен выбросить IllegalArgumentException, если rangeStart после rangeEnd")
        void getEventsAdmin_whenRangeStartIsAfterRangeEnd_shouldThrowIllegalArgumentException() {
            LocalDateTime rangeStart = plusTwoHours; // now.plusHours(2)
            LocalDateTime rangeEnd = plusOneHour;   // now.plusHours(1)
            List<Long> users = null;
            List<EventState> states = null;
            List<Long> categories = null;
            int from = 0;
            int size = 10;

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.getEventsAdmin(users, states, categories, rangeStart, rangeEnd,
                    from, size));

            assertEquals("Admin search: rangeStart cannot be after rangeEnd.", exception.getMessage());

            verifyNoInteractions(eventRepository);
            verifyNoInteractions(eventMapper);
        }
    }

    // ... TODO: Добавить тесты для других методов EventService, когда они появятся ...
}