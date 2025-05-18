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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.explorewithme.main.dto.EventFullDto;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.model.Location;
import ru.practicum.explorewithme.main.model.User;

@ExtendWith(MockitoExtension.class)
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

            assertEquals(0L, dto.getConfirmedRequests());
            assertEquals(0L, dto.getViews());
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
}