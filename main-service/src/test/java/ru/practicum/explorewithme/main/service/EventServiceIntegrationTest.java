package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.NewEventDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminEventSearchParams;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Интеграционное тестирование EventServiceImpl")
class EventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16.1");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1, user2;
    private Category category1, category2;
    private Location location1;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        user1 = userRepository.save(User.builder().name("User One").email("user1@events.com").build());
        user2 = userRepository.save(User.builder().name("User Two").email("user2@events.com").build());

        category1 = categoryRepository.save(Category.builder().name("Category A").build());
        category2 = categoryRepository.save(Category.builder().name("Category B").build());

        location1 = Location.builder().lat(10f).lon(10f).build();
    }

    @Nested
    @DisplayName("Метод addEventPrivate")
    class AddEventPrivateTests {

        @Test
        @DisplayName("Должен успешно создавать событие")
        void addEventPrivate_whenDataIsValid_thenEventIsCreated() {
            NewEventDto newEventDto = NewEventDto.builder()
                .annotation("Valid Annotation")
                .category(category1.getId())
                .description("Valid Description")
                .eventDate(now.plusHours(3))
                .location(location1)
                .paid(false)
                .participantLimit(10L)
                .requestModeration(true)
                .title("Valid Event Title")
                .build();

            EventFullDto createdEventDto = eventService.addEventPrivate(user1.getId(), newEventDto);

            assertNotNull(createdEventDto);
            assertNotNull(createdEventDto.getId());
            assertEquals(newEventDto.getAnnotation(), createdEventDto.getAnnotation());
            assertEquals(user1.getId(), createdEventDto.getInitiator().getId());
            assertEquals(category1.getId(), createdEventDto.getCategory().getId());
            assertEquals(EventState.PENDING, createdEventDto.getState());
            assertNotNull(createdEventDto.getCreatedOn()); // Проверяем, что дата создания установлена (JPA Auditing)

            assertTrue(eventRepository.existsById(createdEventDto.getId()));
        }

        @Test
        @DisplayName("Должен выбрасывать EntityNotFoundException, если пользователь не найден")
        void addEventPrivate_whenUserNotFound_thenThrowsEntityNotFoundException() {
            Long nonExistentUserId = 999L;
            NewEventDto newEventDto = NewEventDto.builder().category(category1.getId()).eventDate(now.plusHours(3))
                .annotation("A").description("D").title("T").location(location1).build();

            assertThrows(EntityNotFoundException.class, () ->
                eventService.addEventPrivate(nonExistentUserId, newEventDto));
        }

        @Test
        @DisplayName("Должен выбрасывать EntityNotFoundException, если категория не найдена")
        void addEventPrivate_whenCategoryNotFound_thenThrowsEntityNotFoundException() {
            Long nonExistentCategoryId = 888L;
            NewEventDto newEventDto = NewEventDto.builder().category(nonExistentCategoryId).eventDate(now.plusHours(3))
                .annotation("A").description("D").title("T").location(location1).build();

            assertThrows(EntityNotFoundException.class, () ->
                eventService.addEventPrivate(user1.getId(), newEventDto));
        }

        @Test
        @DisplayName("Должен выбрасывать BusinessRuleViolationException, если дата события слишком ранняя")
        void addEventPrivate_whenEventDateIsTooSoon_thenThrowsBusinessRuleViolationException() {
            NewEventDto newEventDto = NewEventDto.builder().category(category1.getId()).eventDate(now.plusHours(1))
                .annotation("A").description("D").title("T").location(location1).build();

            assertThrows(BusinessRuleViolationException.class, () ->
                eventService.addEventPrivate(user1.getId(), newEventDto));
        }
    }

    @Nested
    @DisplayName("Метод getEventsAdmin")
    class GetEventsAdminTests {

        @BeforeEach
        void setUpAdminEvents() {
            Event event1 = Event.builder().title("Admin Event 1").annotation("A1").description("D1")
                .category(category1).initiator(user1).location(location1)
                .eventDate(now.plusDays(5)).state(EventState.PENDING).createdOn(now.minusDays(1)).build();
            Event event2 = Event.builder().title("Admin Event 2").annotation("A2").description("D2")
                .category(category2).initiator(user2).location(location1)
                .eventDate(now.plusDays(10)).state(EventState.PUBLISHED).createdOn(now.minusDays(2)).build();
            Event event3 = Event.builder().title("Admin Event 3").annotation("Another A").description("Another D")
                .category(category1).initiator(user1).location(location1)
                .eventDate(now.plusDays(15)).state(EventState.PUBLISHED).createdOn(now.minusDays(3)).build();
            eventRepository.saveAll(List.of(event1, event2, event3));
        }

        @Test
        @DisplayName("Должен вернуть все события с пагинацией при отсутствии фильтров")
        void getEventsAdmin_whenNoFilters_thenReturnsAllEventsPaged() {
            AdminEventSearchParams params = AdminEventSearchParams.builder().build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 2);
            assertEquals(2, result.size());

            List<EventFullDto> resultNextPage = eventService.getEventsAdmin(params, 2, 2);
            assertEquals(1, resultNextPage.size());
        }

        @Test
        @DisplayName("Должен вернуть соответствующие события при поиске с фильтром по пользователям")
        void getEventsAdmin_whenUserFilterApplied_thenReturnsMatchingEvents() {
            AdminEventSearchParams params = AdminEventSearchParams.builder().users(List.of(user1.getId())).build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 10);
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(e -> e.getInitiator().getId().equals(user1.getId())));
        }

        @Test
        @DisplayName("Должен вернуть соответствующие события при поиске с фильтром по состояниям")
        void getEventsAdmin_whenStateFilterApplied_thenReturnsMatchingEvents() {
            AdminEventSearchParams params = AdminEventSearchParams.builder().states(List.of(EventState.PUBLISHED)).build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 10);
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(e -> e.getState() == EventState.PUBLISHED));
        }

        @Test
        @DisplayName("Должен вернуть соответствующие события при поиске с фильтром по категориям")
        void getEventsAdmin_whenCategoryFilterApplied_thenReturnsMatchingEvents() {
            AdminEventSearchParams params = AdminEventSearchParams.builder().categories(List.of(category1.getId())).build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 10);
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(e -> e.getCategory().getId().equals(category1.getId())));
        }

        @Test
        @DisplayName("Должен вернуть соответствующие события при поиске с фильтром по диапазону дат")
        void getEventsAdmin_whenDateRangeFilterApplied_thenReturnsMatchingEvents() {
            AdminEventSearchParams params = AdminEventSearchParams.builder()
                .rangeStart(now.plusDays(7))
                .rangeEnd(now.plusDays(12))
                .build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 10);
            assertEquals(1, result.size());
            assertEquals("Admin Event 2", result.getFirst().getTitle());
        }

        @Test
        @DisplayName("Должен выбрасывать IllegalArgumentException при поиске с невалидным диапазоном дат")
        void getEventsAdmin_whenInvalidDateRange_thenThrowsIllegalArgumentException() {
            AdminEventSearchParams params = AdminEventSearchParams.builder()
                .rangeStart(now.plusDays(10))
                .rangeEnd(now.plusDays(5))
                .build();
            assertThrows(IllegalArgumentException.class, () -> eventService.getEventsAdmin(params, 0, 10));
        }

        @Test
        @DisplayName("Должен вернуть пустой список при поиске без совпадающих критериев")
        void getEventsAdmin_whenNoEventsMatchCriteria_thenReturnsEmptyList() {
            AdminEventSearchParams params = AdminEventSearchParams.builder().users(List.of(999L)).build();
            List<EventFullDto> result = eventService.getEventsAdmin(params, 0, 10);
            assertTrue(result.isEmpty());
        }
    }
}