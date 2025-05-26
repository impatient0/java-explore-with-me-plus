package ru.practicum.explorewithme.main.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.querydsl.core.types.Predicate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.dto.UpdateEventAdminRequestDto;
import ru.practicum.explorewithme.main.dto.UpdateEventUserRequestDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.EventMapper;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для реализации EventService")
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Captor
    private ArgumentCaptor<Predicate> predicateCaptor;

    @Captor
    private ArgumentCaptor<Event> eventArgumentCaptor;

    private LocalDateTime now;
    private LocalDateTime plusOneHour;
    private LocalDateTime plusTwoHours;
    private LocalDateTime plusThreeHours;
    private QEvent qEvent;

    private User testUser;
    private Category testCategory;
    private NewEventDto newEventDto;
    private Event mappedEventFromDto;
    private Event savedEvent;
    private EventFullDto eventFullDto;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        plusOneHour = now.plusHours(1);
        plusTwoHours = now.plusHours(2);
        plusThreeHours = now.plusHours(3);
        qEvent = QEvent.event;

        testUser = User.builder().id(1L).name("Test User").build();
        testCategory = Category.builder().id(10L).name("Test Category").build();

        newEventDto = NewEventDto.builder()
            .annotation("New Event Annotation")
            .category(testCategory.getId())
            .description("New Event Description")
            .eventDate(plusThreeHours)
            .location(Location.builder().lat(10f).lon(20f).build())
            .paid(false)
            .participantLimit(0L)
            .requestModeration(true)
            .title("New Event Title")
            .build();

        mappedEventFromDto = Event.builder()
            .annotation(newEventDto.getAnnotation())
            .category(Category.builder().id(newEventDto.getCategory()).build())
            .description(newEventDto.getDescription())
            .eventDate(newEventDto.getEventDate())
            .location(newEventDto.getLocation())
            .paid(newEventDto.getPaid())
            .participantLimit(newEventDto.getParticipantLimit().intValue())
            .requestModeration(newEventDto.getRequestModeration())
            .title(newEventDto.getTitle())
            .build();

        savedEvent = Event.builder()
            .id(1L)
            .annotation(newEventDto.getAnnotation())
            .category(testCategory)
            .description(newEventDto.getDescription())
            .eventDate(newEventDto.getEventDate())
            .initiator(testUser)
            .location(newEventDto.getLocation())
            .paid(newEventDto.getPaid())
            .participantLimit(newEventDto.getParticipantLimit().intValue())
            .requestModeration(newEventDto.getRequestModeration())
            .title(newEventDto.getTitle())
            .createdOn(now)
            .state(EventState.PENDING)
            .build();

        eventFullDto = EventFullDto.builder().id(1L).title("New Event Title").build();
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

            AdminEventSearchParams params = AdminEventSearchParams.builder().users(users).build();
            eventService.getEventsAdmin(params, 0, 10);

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

            AdminEventSearchParams params = AdminEventSearchParams.builder().states(states).build();
            eventService.getEventsAdmin(params, 0, 10);

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

            AdminEventSearchParams params = AdminEventSearchParams.builder().categories(categories).build();
            eventService.getEventsAdmin(params, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();

            String categoryIdPath = qEvent.category.id.toString();

            assertTrue(predicateString.contains(categoryIdPath) && predicateString.contains("5"),
                "Предикат должен содержать фильтр по ID категорий: " + predicateString);
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }


        @Test
        @DisplayName("Должен формировать предикат, если передана начальная дата диапазона")
        void getEventsAdmin_withRangeStart_shouldApplyRangeStartPredicate() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));
            Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(eventRepository.findAll(predicateCaptor.capture(), eq(pageable))).thenReturn(
                emptyPage);

            AdminEventSearchParams params = AdminEventSearchParams.builder().rangeStart(now).build();
            eventService.getEventsAdmin(params, 0, 10);

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

            AdminEventSearchParams params = AdminEventSearchParams.builder().rangeEnd(plusTwoHours).build();
            eventService.getEventsAdmin(params, 0, 10);

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

            AdminEventSearchParams params = AdminEventSearchParams.builder().build();
            eventService.getEventsAdmin(params, 0, 10);

            ArgumentCaptor<Predicate> localPredicateCaptor = ArgumentCaptor.forClass(Predicate.class);
            verify(eventRepository).findAll(localPredicateCaptor.capture(), eq(pageable));

            Predicate capturedPredicate = localPredicateCaptor.getValue();
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

            AdminEventSearchParams params = AdminEventSearchParams.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();
            eventService.getEventsAdmin(params, 0, 10);

            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateString = capturedPredicate.toString();

            String initiatorIdPath = qEvent.initiator.id.toString();
            String statePath = qEvent.state.toString();
            String categoryIdPath = qEvent.category.id.toString();
            String eventDatePath = qEvent.eventDate.toString();

            assertAll("Проверка всех частей предиката",
                () -> assertTrue(
                    predicateString.contains(initiatorIdPath) && predicateString.contains(users.getFirst().toString()),
                    "Фильтр по пользователям: " + predicateString),
                () -> assertTrue(
                    predicateString.contains(statePath) && predicateString.contains(states.getFirst().toString()),
                    "Фильтр по состояниям: " + predicateString),
                () -> assertTrue(
                    predicateString.contains(categoryIdPath) && predicateString.contains(categories.getFirst().toString()),
                    "Фильтр по категориям: " + predicateString),
                () -> assertTrue(
                    predicateString.contains(eventDatePath) && predicateString.contains(">= " + rangeStart.toString()),
                    "Фильтр по начальной дате: " + predicateString),
                () -> assertTrue(
                    predicateString.contains(eventDatePath) && predicateString.contains("<= " + rangeEnd.toString()),
                    "Фильтр по конечной дате: " + predicateString)
            );
            verify(eventRepository).findAll(capturedPredicate, pageable);
        }

        @Test
        @DisplayName("Должен выбросить IllegalArgumentException, если rangeStart после rangeEnd")
        void getEventsAdmin_whenRangeStartIsAfterRangeEnd_shouldThrowIllegalArgumentException() {
            LocalDateTime rangeStart = plusTwoHours; // now.plusHours(2)
            LocalDateTime rangeEnd = plusOneHour;   // now.plusHours(1)
            int from = 0;
            int size = 10;

            AdminEventSearchParams params = AdminEventSearchParams.builder()
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .build();

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.getEventsAdmin(params, from, size));

            assertEquals("Admin search: rangeStart cannot be after rangeEnd.", exception.getMessage());

            verifyNoInteractions(eventRepository);
            verifyNoInteractions(eventMapper);
        }
    }

    @Nested
    @DisplayName("Метод addEventPrivate")
    class AddEventPrivateTests {

        @Test
        @DisplayName("Должен успешно создавать событие")
        void addEventPrivate_whenDataIsValid_shouldCreateAndReturnEventFullDto() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(eventMapper.toEvent(newEventDto)).thenReturn(mappedEventFromDto);
            when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
            when(eventMapper.toEventFullDto(savedEvent)).thenReturn(eventFullDto);

            EventFullDto result = eventService.addEventPrivate(testUser.getId(), newEventDto);

            assertNotNull(result);
            assertEquals(eventFullDto.getId(), result.getId());
            assertEquals(eventFullDto.getTitle(), result.getTitle());

            verify(userRepository).findById(testUser.getId());
            verify(categoryRepository).findById(testCategory.getId());
            verify(eventMapper).toEvent(newEventDto);
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertEquals(testUser, capturedEvent.getInitiator(), "Инициатор должен быть установлен в сервисе");

            verify(eventMapper).toEventFullDto(savedEvent);
        }

        @Test
        @DisplayName("Должен выбрасывать EntityNotFoundException, если пользователь не найден")
        void addEventPrivate_whenUserNotFound_shouldThrowEntityNotFoundException() {
            Long nonExistentUserId = 999L;
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> eventService.addEventPrivate(nonExistentUserId, newEventDto));

            assertTrue(exception.getMessage().contains("Пользователь"));
            assertTrue(exception.getMessage().contains(nonExistentUserId.toString()));
            verify(userRepository).findById(nonExistentUserId);
            verifyNoInteractions(categoryRepository, eventRepository, eventMapper);
        }

        @Test
        @DisplayName("Должен выбрасывать EntityNotFoundException, если категория не найдена")
        void addEventPrivate_whenCategoryNotFound_shouldThrowEntityNotFoundException() {
            Long nonExistentCategoryId = 888L;
            NewEventDto dtoWithNonExistentCategory = NewEventDto.builder()
                .category(nonExistentCategoryId)
                .annotation("A").description("D").title("T").eventDate(plusThreeHours)
                .location(Location.builder().lat(1f).lon(1f).build())
                .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> eventService.addEventPrivate(testUser.getId(), dtoWithNonExistentCategory));

            assertTrue(exception.getMessage().contains("Категория"));
            assertTrue(exception.getMessage().contains(nonExistentCategoryId.toString()));
            verify(userRepository).findById(testUser.getId());
            verify(categoryRepository).findById(nonExistentCategoryId);
            verifyNoInteractions(eventRepository, eventMapper);
        }

        @Test
        @DisplayName("Должен выбрасывать BusinessRuleViolationException, если дата события слишком ранняя")
        void addEventPrivate_whenEventDateIsTooSoon_shouldThrowBusinessRuleViolationException() {
            NewEventDto dtoWithEarlyDate = NewEventDto.builder()
                .category(testCategory.getId())
                .eventDate(now.plusHours(1)) // Меньше чем через 2 часа
                .annotation("A").description("D").title("T")
                .location(Location.builder().lat(1f).lon(1f).build())
                .build();

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));

            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> eventService.addEventPrivate(testUser.getId(), dtoWithEarlyDate));
            assertTrue(exception.getMessage().contains("должна быть не ранее, чем через 2 часа"));

            verify(userRepository).findById(testUser.getId());
            verify(categoryRepository).findById(testCategory.getId());
            verifyNoInteractions(eventRepository, eventMapper);
        }

        @Test
        @DisplayName("Должен корректно устанавливать инициатора и категорию в событие перед сохранением")
        void addEventPrivate_shouldSetInitiatorAndCategoryCorrectly() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(categoryRepository.findById(testCategory.getId())).thenReturn(Optional.of(testCategory));
            when(eventMapper.toEvent(newEventDto)).thenReturn(mappedEventFromDto);
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(eventFullDto);

            eventService.addEventPrivate(testUser.getId(), newEventDto);

            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();

            assertEquals(testUser, capturedEvent.getInitiator(), "Инициатор должен быть корректно установлен.");
            assertEquals(testCategory.getId(), capturedEvent.getCategory().getId(), "ID категории должен быть корректно установлен маппером.");
        }
    }

    @Nested
    @DisplayName("Метод getEventsByOwner")
    class GetEventsByOwnerTests {
        private Long ownerId;
        private Long nonExistentOwnerId;
        private Pageable defaultPageable;
        private Event event1Owned, event2Owned;

        @BeforeEach
        void setUpOwnerEvents() {
            ownerId = testUser.getId();
            nonExistentOwnerId = 999L;
            defaultPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "eventDate"));

            event1Owned = Event.builder().id(101L).title("Owned Event 1").initiator(testUser).category(testCategory)
                .eventDate(now.plusDays(1)).state(EventState.PENDING).createdOn(now).build();
            event2Owned = Event.builder().id(102L).title("Owned Event 2").initiator(testUser).category(testCategory)
                .eventDate(now.plusDays(2)).state(EventState.PUBLISHED).createdOn(now).build();
        }

        @Test
        @DisplayName("Должен возвращать список EventShortDto событий пользователя с пагинацией")
        void getEventsByOwner_whenUserExistsAndHasEvents_shouldReturnEventShortDtoList() {
            List<Event> eventsFromRepo = List.of(event2Owned, event1Owned);
            Page<Event> eventPage = new PageImpl<>(eventsFromRepo, defaultPageable, eventsFromRepo.size());

            List<EventShortDto> expectedDtos = List.of(
                EventShortDto.builder().id(102L).title("Owned Event 2").build(),
                EventShortDto.builder().id(101L).title("Owned Event 1").build()
            );

            when(userRepository.existsById(ownerId)).thenReturn(true);
            when(eventRepository.findByInitiatorId(ownerId, defaultPageable)).thenReturn(eventPage);
            when(eventMapper.toEventShortDtoList(eventsFromRepo)).thenReturn(expectedDtos);

            List<EventShortDto> result = eventService.getEventsByOwner(ownerId, 0, 10);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(expectedDtos.get(0).getTitle(), result.get(0).getTitle());
            assertEquals(expectedDtos.get(1).getTitle(), result.get(1).getTitle());

            verify(userRepository).existsById(ownerId);
            verify(eventRepository).findByInitiatorId(ownerId, defaultPageable);
            verify(eventMapper).toEventShortDtoList(eventsFromRepo);
        }

        @Test
        @DisplayName("Должен возвращать пустой список, если у пользователя нет событий")
        void getEventsByOwner_whenUserHasNoEvents_shouldReturnEmptyList() {
            when(userRepository.existsById(ownerId)).thenReturn(true);
            when(eventRepository.findByInitiatorId(ownerId, defaultPageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), defaultPageable, 0));

            List<EventShortDto> result = eventService.getEventsByOwner(ownerId, 0, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(userRepository).existsById(ownerId);
            verify(eventRepository).findByInitiatorId(ownerId, defaultPageable);
        }

        @Test
        @DisplayName("Должен возвращать пустой список, если пользователь не найден")
        void getEventsByOwner_whenUserNotFound_shouldThrowEntityNotFoundException() {
            when(userRepository.existsById(nonExistentOwnerId)).thenReturn(false);

            List<EventShortDto> result = eventService.getEventsByOwner(nonExistentOwnerId, 0, 10);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Метод getEventPrivate")
    class GetEventPrivateTests {
        private Long ownerId;
        private Long eventIdOwned;
        private Long eventIdNotOwned;
        private Long nonExistentEventId;
        private Event ownedEvent;
        private EventFullDto ownedEventFullDto;

        @BeforeEach
        void setUpPrivateEvent() {
            ownerId = testUser.getId();
            eventIdOwned = savedEvent.getId();
            eventIdNotOwned = 998L;
            nonExistentEventId = 999L;

            ownedEvent = Event.builder()
                .id(eventIdOwned)
                .title(savedEvent.getTitle())
                .initiator(testUser)
                .category(testCategory)
                .eventDate(savedEvent.getEventDate())
                .state(EventState.PENDING)
                .build();

            ownedEventFullDto = EventFullDto.builder()
                .id(eventIdOwned)
                .title(ownedEvent.getTitle())
                .build();
        }

        @Test
        @DisplayName("Должен возвращать EventFullDto, если событие найдено и принадлежит пользователю")
        void getEventPrivate_whenEventFoundAndOwned_shouldReturnEventFullDto() {
            when(userRepository.existsById(ownerId)).thenReturn(true);
            when(eventRepository.findByIdAndInitiatorId(eventIdOwned, ownerId))
                .thenReturn(Optional.of(ownedEvent));
            when(eventMapper.toEventFullDto(ownedEvent)).thenReturn(ownedEventFullDto);

            EventFullDto result = eventService.getEventPrivate(ownerId, eventIdOwned);

            assertNotNull(result);
            assertEquals(ownedEventFullDto.getId(), result.getId());
            assertEquals(ownedEventFullDto.getTitle(), result.getTitle());

            verify(userRepository).existsById(ownerId);
            verify(eventRepository).findByIdAndInitiatorId(eventIdOwned, ownerId);
            verify(eventMapper).toEventFullDto(ownedEvent);
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если пользователь не найден")
        void getEventPrivate_whenUserNotFound_shouldThrowEntityNotFoundException() {
            Long nonExistentUserId = 888L;
            when(userRepository.existsById(nonExistentUserId)).thenReturn(false);

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.getEventPrivate(nonExistentUserId, eventIdOwned);
            });
            assertTrue(exception.getMessage().contains("User with id=" + nonExistentUserId + " not found"));
            verify(userRepository).existsById(nonExistentUserId);
            verifyNoInteractions(eventRepository, eventMapper);
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если событие не найдено (или не принадлежит пользователю)")
        void getEventPrivate_whenEventNotFoundOrNotOwned_shouldThrowEntityNotFoundException() {
            when(userRepository.existsById(ownerId)).thenReturn(true);
            when(eventRepository.findByIdAndInitiatorId(nonExistentEventId, ownerId))
                .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.getEventPrivate(ownerId, nonExistentEventId);
            });
            assertTrue(exception.getMessage().contains("Event with id=" + nonExistentEventId));
            assertTrue(exception.getMessage().contains("initiatorId=" + ownerId));

            verify(userRepository).existsById(ownerId);
            verify(eventRepository).findByIdAndInitiatorId(nonExistentEventId, ownerId);
            verifyNoInteractions(eventMapper);
        }
    }

    @Nested
    @DisplayName("Метод updateEventByOwner")
    class UpdateEventByOwnerTests {

        private Long existingEventId;
        private UpdateEventUserRequestDto validUpdateDto;
        private Event existingEvent;
        private Event updatedEventFromRepo;
        private EventFullDto updatedEventFullDto;

        @BeforeEach
        void setUpUpdateTests() {
            existingEventId = savedEvent.getId();

            validUpdateDto = UpdateEventUserRequestDto.builder()
                .title("Updated Event Title")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .eventDate(now.plusDays(10)) // Валидная дата (дальше чем +2 часа от now)
                .paid(true)
                .participantLimit(50)
                .requestModeration(false)
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            existingEvent = Event.builder()
                .id(existingEventId)
                .title("Original Title")
                .annotation("Original Annotation")
                .description("Original Description")
                .eventDate(now.plusDays(5))
                .initiator(testUser)
                .category(testCategory)
                .location(Location.builder().lat(10f).lon(10f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .createdOn(now.minusDays(1))
                .build();

            updatedEventFromRepo = Event.builder()
                .id(existingEvent.getId())
                .title(validUpdateDto.getTitle())
                .annotation(validUpdateDto.getAnnotation())
                .description(validUpdateDto.getDescription())
                .eventDate(validUpdateDto.getEventDate())
                .paid(validUpdateDto.getPaid())
                .participantLimit(validUpdateDto.getParticipantLimit())
                .requestModeration(validUpdateDto.getRequestModeration())
                .state(EventState.PENDING) // SEND_TO_REVIEW оставляет PENDING
                .initiator(existingEvent.getInitiator())
                .category(existingEvent.getCategory())
                .location(existingEvent.getLocation())
                .createdOn(existingEvent.getCreatedOn())
                .build();

            updatedEventFullDto = EventFullDto.builder()
                .id(updatedEventFromRepo.getId())
                .title(updatedEventFromRepo.getTitle())
                .build();
        }

        @Test
        @DisplayName("Должен успешно обновлять событие, если все условия соблюдены")
        void updateEventByOwner_whenValidRequestAndState_shouldUpdateAndReturnDto() {
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(updatedEventFromRepo);
            when(eventMapper.toEventFullDto(updatedEventFromRepo)).thenReturn(updatedEventFullDto);

            EventFullDto result = eventService.updateEventByOwner(testUser.getId(), existingEventId, validUpdateDto);

            assertNotNull(result);
            assertEquals(updatedEventFullDto.getId(), result.getId());
            assertEquals(validUpdateDto.getTitle(), result.getTitle());

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEntity = eventArgumentCaptor.getValue();
            assertEquals(validUpdateDto.getTitle(), savedEntity.getTitle());
            assertEquals(EventState.PENDING, savedEntity.getState()); // SEND_TO_REVIEW
            assertEquals(validUpdateDto.getPaid(), savedEntity.isPaid());
            assertEquals(validUpdateDto.getParticipantLimit().intValue(), savedEntity.getParticipantLimit());

            verify(eventMapper).toEventFullDto(updatedEventFromRepo);
        }

        @Test
        @DisplayName("Должен обновлять категорию, если она указана в DTO")
        void updateEventByOwner_whenCategoryInDto_shouldUpdateCategory() {
            Category newCategory = Category.builder().id(20L).name("New Test Category").build();
            UpdateEventUserRequestDto dtoWithCategory = UpdateEventUserRequestDto.builder()
                .category(newCategory.getId())
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            Event eventToUpdate = Event.builder() // Копия existingEvent для этого теста
                .id(existingEventId).title("T").annotation("A").description("D").eventDate(now.plusDays(5))
                .initiator(testUser).category(testCategory).location(Location.builder().lat(1f).lon(1f).build())
                .state(EventState.PENDING).build();

            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(eventToUpdate));
            when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Возвращаем измененный event
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(updatedEventFullDto);

            eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoWithCategory);

            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEntity = eventArgumentCaptor.getValue();
            assertEquals(newCategory.getId(), savedEntity.getCategory().getId());
        }


        @Test
        @DisplayName("Должен изменять состояние на CANCELED при stateAction = CANCEL_REVIEW")
        void updateEventByOwner_whenStateActionIsCancelReview_shouldSetStateToCanceled() {
            UpdateEventUserRequestDto dtoCancel = UpdateEventUserRequestDto.builder()
                .stateAction(UpdateEventUserRequestDto.StateActionUser.CANCEL_REVIEW)
                .build();
            // existingEvent уже в PENDING, что позволяет отмену

            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(updatedEventFullDto);

            eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoCancel);

            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEntity = eventArgumentCaptor.getValue();
            assertEquals(EventState.CANCELED, savedEntity.getState());
        }


        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если событие не найдено или не принадлежит пользователю")
        void updateEventByOwner_whenEventNotFoundOrNotOwned_shouldThrowEntityNotFoundException() {
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.updateEventByOwner(testUser.getId(), existingEventId, validUpdateDto);
            });
            assertTrue(exception.getMessage().contains("Event with id=" + existingEventId));
            assertTrue(exception.getMessage().contains("initiatorId=" + testUser.getId()));

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verifyNoInteractions(eventMapper); // save не должен вызываться
        }

        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException, если пытаются обновить опубликованное событие")
        void updateEventByOwner_whenEventIsPublished_shouldThrowBusinessRuleViolationException() {
            existingEvent.setState(EventState.PUBLISHED); // Меняем состояние на PUBLISHED
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.updateEventByOwner(testUser.getId(), existingEventId, validUpdateDto);
            });
            assertTrue(exception.getMessage().contains("Only pending or canceled events can be changed"));

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verifyNoInteractions(eventMapper);
        }

        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException, если eventDate слишком ранняя")
        void updateEventByOwner_whenEventDateIsTooSoon_shouldThrowException() {
            UpdateEventUserRequestDto dtoWithEarlyDate = UpdateEventUserRequestDto.builder()
                .eventDate(now.plusMinutes(30)) // Менее 2 часов
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoWithEarlyDate);
            });
            assertTrue(exception.getMessage().contains("must be at least two hours in the future"));

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verifyNoInteractions(eventMapper);
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если указана несуществующая категория")
        void updateEventByOwner_whenCategoryNotFound_shouldThrowEntityNotFoundException() {
            Long nonExistentCategoryId = 999L;
            UpdateEventUserRequestDto dtoWithNonExistentCategory = UpdateEventUserRequestDto.builder()
                .category(nonExistentCategoryId)
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoWithNonExistentCategory);
            });
            assertTrue(exception.getMessage().contains("Category with id=" + nonExistentCategoryId));

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verify(categoryRepository).findById(nonExistentCategoryId);
            verifyNoInteractions(eventMapper);
        }
    }

    @Nested
    @DisplayName("Метод moderateEventByAdmin")
    class ModerateEventByAdminTests {

        private Long existingEventId;
        private Event existingPendingEvent;
        private Event existingPublishedEvent;
        private UpdateEventAdminRequestDto publishRequestDto;
        private UpdateEventAdminRequestDto rejectRequestDto;
        private EventFullDto mappedEventFullDto;
        private Category newCategory;

        @BeforeEach
        void setUpModerateTests() {
            existingEventId = 1L;

            existingPendingEvent = Event.builder()
                .id(existingEventId)
                .title("Pending Event")
                .annotation("Pending annotation")
                .description("Pending description")
                .category(testCategory)
                .initiator(testUser)
                .location(Location.builder().lat(30f).lon(30f).build())
                .eventDate(now.plusDays(2))
                .createdOn(now.minusDays(1))
                .state(EventState.PENDING)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .build();

            existingPublishedEvent = Event.builder()
                .id(2L) // Другой ID
                .title("Published Event")
                .state(EventState.PUBLISHED)
                .eventDate(now.plusDays(3))
                .category(testCategory)
                .initiator(testUser)
                .location(Location.builder().lat(40f).lon(40f).build())
                .createdOn(now.minusDays(2))
                .publishedOn(now.minusDays(1))
                .build();

            publishRequestDto = UpdateEventAdminRequestDto.builder()
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .build();

            rejectRequestDto = UpdateEventAdminRequestDto.builder()
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.REJECT_EVENT)
                .build();

            mappedEventFullDto = EventFullDto.builder().id(existingEventId).title("Some Title").build();

            newCategory = Category.builder().id(99L).name("New Category For Admin Update").build();

        }

        @Test
        @DisplayName("Должен успешно публиковать PENDING событие, если дата валидна")
        void moderateEventByAdmin_whenPublishPendingEventWithValidDate_shouldPublish() {
            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(mappedEventFullDto);

            EventFullDto result = eventService.moderateEventByAdmin(existingEventId, publishRequestDto);

            assertNotNull(result);
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEvent = eventArgumentCaptor.getValue();
            assertEquals(EventState.PUBLISHED, savedEvent.getState());
            assertNotNull(savedEvent.getPublishedOn());
            assertTrue(savedEvent.getPublishedOn().isAfter(now.minusSeconds(5)) &&
                savedEvent.getPublishedOn().isBefore(now.plusSeconds(5)));
        }

        @Test
        @DisplayName("Должен успешно отклонять PENDING событие")
        void moderateEventByAdmin_whenRejectPendingEvent_shouldCancel() {
            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(mappedEventFullDto);

            EventFullDto result = eventService.moderateEventByAdmin(existingEventId, rejectRequestDto);

            assertNotNull(result);
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEvent = eventArgumentCaptor.getValue();
            assertEquals(EventState.CANCELED, savedEvent.getState());
            assertNull(savedEvent.getPublishedOn()); // publishedOn не должен быть установлен при отклонении PENDING
        }

        @Test
        @DisplayName("Должен обновлять поля события при модерации, если они переданы в DTO")
        void moderateEventByAdmin_whenDtoHasUpdates_shouldUpdateEventFields() {
            UpdateEventAdminRequestDto updateWithFieldsDto = UpdateEventAdminRequestDto.builder()
                .title("Admin Updated Title")
                .annotation("Admin Updated Annotation")
                .category(newCategory.getId())
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .build();

            existingPendingEvent.setEventDate(now.plusHours(2));

            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));
            when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(mappedEventFullDto);

            eventService.moderateEventByAdmin(existingEventId, updateWithFieldsDto);

            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEvent = eventArgumentCaptor.getValue();
            assertEquals("Admin Updated Title", savedEvent.getTitle());
            assertEquals("Admin Updated Annotation", savedEvent.getAnnotation());
            assertEquals(newCategory.getId(), savedEvent.getCategory().getId());
            assertEquals(EventState.PUBLISHED, savedEvent.getState());
        }

        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException при попытке опубликовать не PENDING событие")
        void moderateEventByAdmin_whenPublishNonPendingEvent_shouldThrowBusinessRuleViolationException() {
            existingPendingEvent.setState(EventState.CANCELED);
            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.moderateEventByAdmin(existingEventId, publishRequestDto);
            });
            assertTrue(exception.getMessage().contains("not in the PENDING state"));
            verify(eventRepository).findById(existingEventId);
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException при попытке опубликовать событие со слишком ранней eventDate")
        void moderateEventByAdmin_whenPublishEventWithTooSoonEventDate_shouldThrowBusinessRuleViolationException() {
            existingPendingEvent.setEventDate(now.plusMinutes(30)); // Менее чем за час до "сейчас"
            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.moderateEventByAdmin(existingEventId, publishRequestDto);
            });
            assertTrue(exception.getMessage().contains("Event date must be at least"));
            verify(eventRepository).findById(existingEventId);
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException при попытке опубликовать событие с eventDate из DTO, которая слишком ранняя")
        void moderateEventByAdmin_whenPublishEventWithDtoEventDateTooSoon_shouldThrowBusinessRuleViolationException() {
            UpdateEventAdminRequestDto dtoWithEarlyDate = UpdateEventAdminRequestDto.builder()
                .eventDate(now.plusMinutes(30))
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .build();

            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.moderateEventByAdmin(existingEventId, dtoWithEarlyDate);
            });
            assertTrue(exception.getMessage().contains("Event date must be at least"));
            verify(eventRepository).findById(existingEventId);
            verify(eventRepository, never()).save(any());
        }


        @Test
        @DisplayName("Должен выбросить BusinessRuleViolationException при попытке отклонить уже PUBLISHED событие")
        void moderateEventByAdmin_whenRejectPublishedEvent_shouldThrowBusinessRuleViolationException() {
            when(eventRepository.findById(existingPublishedEvent.getId())).thenReturn(Optional.of(existingPublishedEvent));

            BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> {
                eventService.moderateEventByAdmin(existingPublishedEvent.getId(), rejectRequestDto);
            });
            assertTrue(exception.getMessage().contains("already been published"));
            verify(eventRepository).findById(existingPublishedEvent.getId());
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если событие для модерации не найдено")
        void moderateEventByAdmin_whenEventNotFound_shouldThrowEntityNotFoundException() {
            Long nonExistentEventId = 999L;
            when(eventRepository.findById(nonExistentEventId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.moderateEventByAdmin(nonExistentEventId, publishRequestDto);
            });
            assertTrue(exception.getMessage().contains("Event with id=" + nonExistentEventId + " not found"));
            verify(eventRepository).findById(nonExistentEventId);
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException при обновлении, если категория из DTO не найдена")
        void moderateEventByAdmin_whenUpdateWithNonExistentCategory_shouldThrowEntityNotFoundException() {
            Long nonExistentCategoryId = 888L;
            UpdateEventAdminRequestDto updateDtoWithBadCategory = UpdateEventAdminRequestDto.builder()
                .category(nonExistentCategoryId)
                .stateAction(UpdateEventAdminRequestDto.StateActionAdmin.PUBLISH_EVENT)
                .build();

            existingPendingEvent.setEventDate(now.plusHours(2));

            when(eventRepository.findById(existingEventId)).thenReturn(Optional.of(existingPendingEvent));
            when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.moderateEventByAdmin(existingEventId, updateDtoWithBadCategory);
            });
            assertTrue(exception.getMessage().contains("Category with id=" + nonExistentCategoryId));
            verify(eventRepository).findById(existingEventId);
            verify(categoryRepository).findById(nonExistentCategoryId);
            verify(eventRepository, never()).save(any());
        }
    }
}