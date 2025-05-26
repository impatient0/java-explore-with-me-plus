package ru.practicum.explorewithme.main.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.dto.EventShortDto;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.model.Location;
import ru.practicum.explorewithme.main.model.User;

@DisplayName("Тесты для EventMapper")
@ActiveProfiles("mapper_test")
@SpringBootTest
class EventMapperTest {

    @Autowired // Внедряем экземпляр, созданный Spring и MapStruct
    private EventMapper eventMapper;

    @Nested
    @DisplayName("Метод toEventFullDto (маппинг одиночного события в EventFullDto)")
    class ToEventFullDtoTests {

        @Test
        @DisplayName("Должен корректно маппить все поля, когда все данные присутствуют")
        void toEventFullDto_shouldMapAllFieldsCorrectly() {
            User initiatorModel = User.builder().id(1L).name("Test User").email("user@test.com").build();
            Category categoryModel = Category.builder().id(10L).name("Test Category").build();
            Location locationModel = Location.builder().lat(55.75f).lon(37.62f).build();

            Event event = Event.builder()
                .id(1L)
                .annotation("Test Annotation")
                .category(categoryModel)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(5))
                .initiator(initiatorModel)
                .location(locationModel)
                .paid(true)
                .participantLimit(100)
                .publishedOn(LocalDateTime.now())
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Test Event Title")
                .confirmedRequestsCount(42L)
                .build();

            EventFullDto dto = eventMapper.toEventFullDto(event);

            assertNotNull(dto);
            assertEquals(event.getId(), dto.getId());
            assertEquals(event.getAnnotation(), dto.getAnnotation());
            assertEquals(event.getCreatedOn(), dto.getCreatedOn());
            assertEquals(event.getDescription(), dto.getDescription());
            assertEquals(event.getEventDate(), dto.getEventDate());
            assertEquals(event.isPaid(), dto.isPaid());
            assertEquals(event.getParticipantLimit(), dto.getParticipantLimit());
            assertEquals(event.getPublishedOn(), dto.getPublishedOn());
            assertEquals(event.isRequestModeration(), dto.isRequestModeration());
            assertEquals(event.getState(), dto.getState());
            assertEquals(event.getTitle(), dto.getTitle());


            assertNotNull(dto.getCategory());
            assertEquals(categoryModel.getId(), dto.getCategory().getId());
            assertEquals(categoryModel.getName(), dto.getCategory().getName());

            assertNotNull(dto.getInitiator());
            assertEquals(initiatorModel.getId(), dto.getInitiator().getId());
            assertEquals(initiatorModel.getName(), dto.getInitiator().getName());

            assertNotNull(dto.getLocation());
            assertEquals(locationModel.getLat(), dto.getLocation().getLat());
            assertEquals(locationModel.getLon(), dto.getLocation().getLon());

            assertEquals(event.getConfirmedRequestsCount(), dto.getConfirmedRequests());

            // Не мапит просмотры и потдверждённые запросы.
            assertNull(dto.getViews());
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null Event")
        void toEventFullDto_shouldHandleNullEvent() {
            EventFullDto dto = eventMapper.toEventFullDto(null);
            assertNull(dto);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать null для вложенных объектов (категория, инициатор, локация)")
        void toEventFullDto_shouldHandleNullNestedObjects() {
            Event event = Event.builder()
                .id(1L)
                .annotation("Test Annotation")
                // category, initiator, location остаются null
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Test Description")
                .eventDate(LocalDateTime.now().plusDays(5))
                .paid(true)
                .participantLimit(100)
                .publishedOn(LocalDateTime.now())
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Test Event Title")
                .build();

            EventFullDto dto = eventMapper.toEventFullDto(event);

            assertNotNull(dto);
            assertNull(dto.getCategory());
            assertNull(dto.getInitiator());
            assertNull(dto.getLocation());
        }
    }


    @Nested
    @DisplayName("Метод toEventFullDtoList (маппинг списка событий в список EventFullDto)")
    class ToEventFullDtoListTests {

        @Test
        @DisplayName("должен корректно маппить список событий")
        void toEventFullDtoList_shouldMapListOfEvents() {
            User initiatorModel = User.builder().id(1L).name("Test User").build();
            Category categoryModel = Category.builder().id(10L).name("Test Category").build();
            Location locationModel = Location.builder().lat(55.75f).lon(37.62f).build();

            Event event1 = Event.builder().id(1L).title("Event 1").category(categoryModel).initiator(initiatorModel).location(locationModel).eventDate(LocalDateTime.now()).createdOn(LocalDateTime.now()).annotation("A1").description("D1").state(EventState.PENDING).paid(false).participantLimit(10).requestModeration(false).publishedOn(null).build();
            Event event2 = Event.builder().id(2L).title("Event 2").category(categoryModel).initiator(initiatorModel).location(locationModel).eventDate(LocalDateTime.now()).createdOn(LocalDateTime.now()).annotation("A2").description("D2").state(EventState.PUBLISHED).paid(true).participantLimit(20).requestModeration(true).publishedOn(LocalDateTime.now()).build();
            List<Event> events = Arrays.asList(event1, event2);

            List<EventFullDto> dtoList = eventMapper.toEventFullDtoList(events);

            assertNotNull(dtoList);
            assertEquals(2, dtoList.size());

            // Проверки для первого элемента списка
            EventFullDto dto1 = dtoList.get(0);
            assertEquals(event1.getTitle(), dto1.getTitle());
            assertNotNull(dto1.getCategory());
            assertEquals(categoryModel.getName(), dto1.getCategory().getName());
            assertNotNull(dto1.getInitiator());
            assertEquals(initiatorModel.getName(), dto1.getInitiator().getName());

            // Проверки для второго элемента списка
            EventFullDto dto2 = dtoList.get(1);
            assertEquals(event2.getTitle(), dto2.getTitle());
            assertNotNull(dto2.getCategory());
            assertEquals(categoryModel.getName(), dto2.getCategory().getName());
            assertNotNull(dto2.getInitiator());
            assertEquals(initiatorModel.getName(), dto2.getInitiator().getName());
        }

        @Test
        @DisplayName("должен возвращать null, если на вход подан null список")
        void toEventFullDtoList_shouldHandleNullList() {
            List<EventFullDto> dtoList = eventMapper.toEventFullDtoList(null);
            assertNull(dtoList);
        }

        @Test
        @DisplayName("должен возвращать пустой список, если на вход подан пустой список")
        void toEventFullDtoList_shouldHandleEmptyList() {
            List<EventFullDto> dtoList = eventMapper.toEventFullDtoList(Collections.emptyList());
            assertNotNull(dtoList);
            assertTrue(dtoList.isEmpty());
        }
    }

    @Nested
    @DisplayName("Метод toEventShortDto (маппинг одиночного события в EventShortDto)")
    class ToEventShortDtoTests {

        @Test
        @DisplayName("Должен корректно маппить поля в EventShortDto")
        void toEventShortDto_shouldMapFieldsCorrectly() {
            User initiatorModel = User.builder().id(1L).name("Test User").build();
            Category categoryModel = Category.builder().id(10L).name("Test Category").build();

            Event event = Event.builder()
                .id(1L)
                .annotation("Short Test Annotation")
                .category(categoryModel)
                .eventDate(LocalDateTime.now().plusDays(5))
                .initiator(initiatorModel)
                .paid(true)
                .title("Short Event Title")
                .confirmedRequestsCount(5L)
                .description("Full description not needed for short dto")
                .state(EventState.PUBLISHED)
                .build();

            EventShortDto dto = eventMapper.toEventShortDto(event);

            assertNotNull(dto);
            assertEquals(event.getId(), dto.getId());
            assertEquals(event.getAnnotation(), dto.getAnnotation());
            assertEquals(event.getEventDate(), dto.getEventDate());
            assertEquals(event.isPaid(), dto.getPaid()); // Используем getPaid() для Boolean из EventShortDto
            assertEquals(event.getTitle(), dto.getTitle());

            assertNotNull(dto.getCategory());
            assertEquals(categoryModel.getId(), dto.getCategory().getId());
            assertEquals(categoryModel.getName(), dto.getCategory().getName());

            assertNotNull(dto.getInitiator());
            assertEquals(initiatorModel.getId(), dto.getInitiator().getId());
            assertEquals(initiatorModel.getName(), dto.getInitiator().getName());

            assertEquals(event.getConfirmedRequestsCount(), dto.getConfirmedRequests());

            assertNull(dto.getViews(), "Views should be null as ignored by mapper and set by service");
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null Event")
        void toEventShortDto_shouldHandleNullEvent() {
            EventShortDto dto = eventMapper.toEventShortDto(null);

            assertNull(dto);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать null для вложенных Category и Initiator")
        void toEventShortDto_shouldHandleNullNestedCategoryAndInitiator() {
            Event event = Event.builder()
                .id(1L)
                .annotation("Annotation with nulls")
                .category(null)
                .eventDate(LocalDateTime.now().plusDays(5))
                .initiator(null)
                .paid(false)
                .title("Event with Nulls")
                .confirmedRequestsCount(33L)
                .build();

            EventShortDto dto = eventMapper.toEventShortDto(event);

            assertNotNull(dto);
            assertNull(dto.getCategory(), "CategoryDto should be null if source category is null");
            assertNull(dto.getInitiator(), "UserShortDto should be null if source initiator is null");
            assertEquals(33L, dto.getConfirmedRequests()); // Проверяем confirmedRequests
        }
    }

    @Nested
    @DisplayName("Метод toEventShortDtoList (маппинг списка событий в список EventShortDto)")
    class ToEventShortDtoListTests {

        @Test
        @DisplayName("Должен корректно маппить список событий в список EventShortDto")
        void toEventShortDtoList_shouldMapListOfEvents() {
            User initiatorModel = User.builder().id(1L).name("Test User").build();
            Category categoryModel = Category.builder().id(10L).name("Test Category").build();

            Event event1 = Event.builder().id(1L).title("Short Event 1").category(categoryModel).initiator(initiatorModel)
                .eventDate(LocalDateTime.now()).annotation("A1").paid(true).confirmedRequestsCount(2L).build();
            Event event2 = Event.builder().id(2L).title("Short Event 2").category(categoryModel).initiator(initiatorModel)
                .eventDate(LocalDateTime.now()).annotation("A2").paid(false).confirmedRequestsCount(5L).build();
            List<Event> events = Arrays.asList(event1, event2);

            List<EventShortDto> dtoList = eventMapper.toEventShortDtoList(events);

            assertNotNull(dtoList);
            assertEquals(2, dtoList.size());

            EventShortDto dto1 = dtoList.get(0);
            assertEquals(event1.getTitle(), dto1.getTitle());
            assertEquals(event1.getConfirmedRequestsCount(), dto1.getConfirmedRequests());
            assertNotNull(dto1.getCategory());
            assertEquals(categoryModel.getName(), dto1.getCategory().getName());

            EventShortDto dto2 = dtoList.get(1);
            assertEquals(event2.getTitle(), dto2.getTitle());
            assertEquals(event2.getConfirmedRequestsCount(), dto2.getConfirmedRequests());
            assertNotNull(dto2.getInitiator());
            assertEquals(initiatorModel.getName(), dto2.getInitiator().getName());
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null список")
        void toEventShortDtoList_shouldHandleNullList() {
            List<EventShortDto> dtoList = eventMapper.toEventShortDtoList(null);

            assertNull(dtoList);
        }

        @Test
        @DisplayName("Должен возвращать пустой список, если на вход подан пустой список")
        void toEventShortDtoList_shouldHandleEmptyList() {
            List<EventShortDto> dtoList = eventMapper.toEventShortDtoList(Collections.emptyList());

            assertNotNull(dtoList);
            assertTrue(dtoList.isEmpty());
        }
    }
}