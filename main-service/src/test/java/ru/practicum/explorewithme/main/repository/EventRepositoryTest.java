package ru.practicum.explorewithme.main.repository;

import com.querydsl.core.BooleanBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.practicum.explorewithme.main.model.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Интеграционные тесты для EventRepository с QueryDSL")
class EventRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:16.1"));

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private User user1, user2;
    private Category category1, category2;
    private Location location1, location2;
    private Event event1, event2, event3, event4;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().withNano(0);

        // Создаем и сохраняем пользователей
        user1 = User.builder().name("User One").email("user1@test.com").build();
        user2 = User.builder().name("User Two").email("user2@test.com").build();
        entityManager.persist(user1);
        entityManager.persist(user2);

        // Создаем и сохраняем категории
        category1 = Category.builder().name("Category One").build();
        category2 = Category.builder().name("Category Two").build();
        entityManager.persist(category1);
        entityManager.persist(category2);

        // Создаем локации (они @Embeddable, не сохраняются отдельно)
        location1 = Location.builder().lat(10.0f).lon(20.0f).build();
        location2 = Location.builder().lat(30.0f).lon(40.0f).build();

        // Создаем и сохраняем события
        event1 = Event.builder()
            .title("Event Alpha")
            .annotation("Annotation for Alpha")
            .description("Description for Alpha")
            .category(category1)
            .initiator(user1)
            .location(location1)
            .eventDate(now.plusDays(5))
            .createdOn(now.minusDays(1))
            .state(EventState.PENDING)
            .paid(false)
            .participantLimit(10)
            .requestModeration(true)
            .build();

        event2 = Event.builder()
            .title("Event Beta")
            .annotation("Annotation for Beta")
            .description("Description for Beta")
            .category(category2)
            .initiator(user1) // тот же user1
            .location(location2)
            .eventDate(now.plusDays(10))
            .createdOn(now.minusDays(2))
            .state(EventState.PUBLISHED)
            .paid(true)
            .participantLimit(0) // без лимита
            .requestModeration(false)
            .build();

        event3 = Event.builder()
            .title("Event Gamma")
            .annotation("Annotation for Gamma")
            .description("Description for Gamma")
            .category(category1)
            .initiator(user2)
            .location(location1)
            .eventDate(now.plusDays(15))
            .createdOn(now.minusDays(3))
            .state(EventState.PUBLISHED)
            .paid(false)
            .participantLimit(5)
            .requestModeration(true)
            .build();

        event4 = Event.builder()
            .title("Event Delta Past Published")
            .annotation("Annotation for Delta")
            .description("Description for Delta")
            .category(category2)
            .initiator(user2)
            .location(location2)
            .eventDate(now.minusDays(1))
            .publishedOn(now.minusDays(2))
            .createdOn(now.minusDays(3))
            .state(EventState.PUBLISHED)
            .paid(true)
            .participantLimit(20)
            .requestModeration(true)
            .build();

        eventRepository.saveAll(List.of(event1, event2, event3, event4));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Поиск без фильтров должен вернуть все события с пагинацией")
    void findAll_withNoFilters_shouldReturnAllEventsPaged() {
        Pageable pageable = PageRequest.of(0, 2);
        BooleanBuilder predicate = new BooleanBuilder();

        Page<Event> result = eventRepository.findAll(predicate, pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(4, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(e -> e.getTitle().equals("Event Alpha")));
    }

    @Test
    @DisplayName("Фильтрация по ID пользователей (users)")
    void findAll_withUserFilter_shouldReturnUserEvents() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(qEvent.initiator.id.in(List.of(user1.getId())));

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(e -> e.getInitiator().getId().equals(user1.getId())));
        assertTrue(result.getContent().stream().anyMatch(e -> e.getTitle().equals("Event Alpha")));
        assertTrue(result.getContent().stream().anyMatch(e -> e.getTitle().equals("Event Beta")));
    }

    @Test
    @DisplayName("Фильтрация по состояниям (states)")
    void findAll_withStateFilter_shouldReturnMatchingStateEvents() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(qEvent.state.in(List.of(EventState.PUBLISHED)));

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        assertEquals(3, result.getTotalElements()); // event2, event3, event4
        assertTrue(result.getContent().stream().allMatch(e -> e.getState() == EventState.PUBLISHED));
    }

    @Test
    @DisplayName("Фильтрация по ID категорий (categories)")
    void findAll_withCategoryFilter_shouldReturnMatchingCategoryEvents() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(qEvent.category.id.in(List.of(category1.getId())));

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements()); // event1, event3
        assertTrue(result.getContent().stream().allMatch(e -> e.getCategory().getId().equals(category1.getId())));
    }

    @Test
    @DisplayName("Фильтрация по начальной дате диапазона (rangeStart)")
    void findAll_withRangeStartFilter_shouldReturnEventsAfterOrOnDate() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        LocalDateTime rangeStart = now.plusDays(12); // Только event3 должен попасть
        predicate.and(qEvent.eventDate.goe(rangeStart));

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Event Gamma", result.getContent().getFirst().getTitle());
    }

    @Test
    @DisplayName("Фильтрация по конечной дате диапазона (rangeEnd)")
    void findAll_withRangeEndFilter_shouldReturnEventsBeforeOrOnDate() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        LocalDateTime rangeEnd = now.plusDays(7); // event1 и event4 (если бы не был в прошлом для другого теста)
        // но event4 уже в прошлом, так что только event1
        predicate.and(qEvent.eventDate.loe(rangeEnd));

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        // event1 (now + 5 days)
        // event4 (now - 1 day)
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(e -> e.getTitle().equals("Event Alpha")));
        assertTrue(result.getContent().stream().anyMatch(e -> e.getTitle().equals("Event Delta Past Published")));
    }

    @Test
    @DisplayName("Комплексная фильтрация (user, state, category, range)")
    void findAll_withMultipleFilters_shouldReturnCorrectEvents() {
        QEvent qEvent = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Ищем события user2, в состоянии PUBLISHED, в category1, в диапазоне дат
        predicate.and(qEvent.initiator.id.eq(user2.getId()));
        predicate.and(qEvent.state.eq(EventState.PUBLISHED));
        predicate.and(qEvent.category.id.eq(category1.getId()));
        predicate.and(qEvent.eventDate.between(now.plusDays(14), now.plusDays(16))); // event3

        assertNotNull(predicate.getValue());
        Page<Event> result = eventRepository.findAll(predicate.getValue(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Event Gamma", result.getContent().getFirst().getTitle());
    }
}