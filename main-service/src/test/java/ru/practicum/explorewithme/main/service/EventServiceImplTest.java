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
import ru.practicum.explorewithme.main.dto.NewEventDto;
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
    @DisplayName("Метод updateEventByOwner")
    class UpdateEventByOwnerTests {

        private Long existingEventId;
        private Long otherUserId;
        private UpdateEventUserRequestDto validUpdateDto;
        private Event existingEvent;
        private Event updatedEventFromRepo;
        private EventFullDto updatedEventFullDto;

        @BeforeEach
        void setUpUpdateTests() {
            existingEventId = savedEvent.getId(); // Используем ID из общего setUp
            otherUserId = 2L; // Другой пользователь

            validUpdateDto = UpdateEventUserRequestDto.builder()
                .title("Updated Event Title")
                .annotation("Updated Annotation")
                .description("Updated Description")
                .eventDate(now.plusDays(10)) // Валидная дата (дальше чем +2 часа от now)
                .paid(true)
                .participantLimit(50)
                .requestModeration(false)
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                // category и location можно оставить null, если не хотим их менять,
                // или задать, если хотим проверить их обновление.
                .build();

            // Существующее событие, которое мы будем "находить" и обновлять
            existingEvent = Event.builder()
                .id(existingEventId)
                .title("Original Title")
                .annotation("Original Annotation")
                .description("Original Description")
                .eventDate(now.plusDays(5))
                .initiator(testUser) // testUser.getId() == 1L
                .category(testCategory)
                .location(Location.builder().lat(10f).lon(10f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING) // Важно для возможности обновления
                .createdOn(now.minusDays(1))
                .build();

            // Это то, что вернет eventRepository.save()
            updatedEventFromRepo = Event.builder() // Копируем и обновляем
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
                .category(existingEvent.getCategory()) // Предположим, категория не менялась
                .location(existingEvent.getLocation()) // Предположим, локация не менялась
                .createdOn(existingEvent.getCreatedOn())
                .build();

            // Это то, что вернет eventMapper.toEventFullDto()
            updatedEventFullDto = EventFullDto.builder()
                .id(updatedEventFromRepo.getId())
                .title(updatedEventFromRepo.getTitle())
                // ... другие поля ...
                .build();
        }

        @Test
        @DisplayName("Должен успешно обновлять событие, если все условия соблюдены")
        void updateEventByOwner_whenValidRequestAndState_shouldUpdateAndReturnDto() {
            // Arrange
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(updatedEventFromRepo);
            when(eventMapper.toEventFullDto(updatedEventFromRepo)).thenReturn(updatedEventFullDto);

            // Act
            EventFullDto result = eventService.updateEventByOwner(testUser.getId(), existingEventId, validUpdateDto);

            // Assert
            assertNotNull(result);
            assertEquals(updatedEventFullDto.getId(), result.getId());
            assertEquals(validUpdateDto.getTitle(), result.getTitle()); // Проверяем, что заголовок обновился

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
            // Arrange
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


            // Act
            eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoWithCategory);

            // Assert
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEntity = eventArgumentCaptor.getValue();
            assertEquals(newCategory.getId(), savedEntity.getCategory().getId());
        }


        @Test
        @DisplayName("Должен изменять состояние на CANCELED при stateAction = CANCEL_REVIEW")
        void updateEventByOwner_whenStateActionIsCancelReview_shouldSetStateToCanceled() {
            // Arrange
            UpdateEventUserRequestDto dtoCancel = UpdateEventUserRequestDto.builder()
                .stateAction(UpdateEventUserRequestDto.StateActionUser.CANCEL_REVIEW)
                .build();
            // existingEvent уже в PENDING, что позволяет отмену

            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(eventMapper.toEventFullDto(any(Event.class))).thenReturn(updatedEventFullDto);

            // Act
            eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoCancel);

            // Assert
            verify(eventRepository).save(eventArgumentCaptor.capture());
            Event savedEntity = eventArgumentCaptor.getValue();
            assertEquals(EventState.CANCELED, savedEntity.getState());
        }


        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если событие не найдено или не принадлежит пользователю")
        void updateEventByOwner_whenEventNotFoundOrNotOwned_shouldThrowEntityNotFoundException() {
            // Arrange
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.empty());

            // Act & Assert
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
            // Arrange
            existingEvent.setState(EventState.PUBLISHED); // Меняем состояние на PUBLISHED
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));

            // Act & Assert
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
            // Arrange
            // Предполагаем, что @TwoHoursLater удалена из DTO и проверка в сервисе
            UpdateEventUserRequestDto dtoWithEarlyDate = UpdateEventUserRequestDto.builder()
                .eventDate(now.plusMinutes(30)) // Менее 2 часов
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            // existingEvent в состоянии PENDING
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            // Здесь не мокаем categoryRepository, так как категория в DTO не передается

            // Act & Assert
            // Ожидаем ConflictException, как указано в вашем сервисном методе
            // Если бы это была ошибка валидации DTO, то был бы другой тип исключения,
            // но мы перенесли проверку в сервис.
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
            // Arrange
            Long nonExistentCategoryId = 999L;
            UpdateEventUserRequestDto dtoWithNonExistentCategory = UpdateEventUserRequestDto.builder()
                .category(nonExistentCategoryId)
                .stateAction(UpdateEventUserRequestDto.StateActionUser.SEND_TO_REVIEW)
                .build();

            // existingEvent в состоянии PENDING
            when(eventRepository.findByIdAndInitiatorId(existingEventId, testUser.getId()))
                .thenReturn(Optional.of(existingEvent));
            when(categoryRepository.findById(nonExistentCategoryId)).thenReturn(Optional.empty());

            // Act & Assert
            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
                eventService.updateEventByOwner(testUser.getId(), existingEventId, dtoWithNonExistentCategory);
            });
            assertTrue(exception.getMessage().contains("Category with id=" + nonExistentCategoryId));

            verify(eventRepository).findByIdAndInitiatorId(existingEventId, testUser.getId());
            verify(categoryRepository).findById(nonExistentCategoryId);
            verifyNoInteractions(eventMapper);
        }
    }

    // ... TODO: Добавить тесты для других методов EventService, когда они появятся ...
}