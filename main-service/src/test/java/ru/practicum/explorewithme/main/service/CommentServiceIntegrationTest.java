package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.model.*;
import ru.practicum.explorewithme.main.repository.CommentRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.practicum.explorewithme.common.constants.DateTimeConstants.DATE_TIME_FORMAT_PATTERN;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("Интеграционное тестирование CommentServiceImpl")
class CommentServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.1")
                    .withDatabaseName("ewm")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CommentService commentService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("Получение комментариев к событию")
    class GetCommentsForEvent {

        @Test
        @DisplayName("Возвращает комментарии опубликованного события с включёнными комментариями")
        void shouldReturnComments_whenEventPublishedAndCommentsEnabled() {

            Event event = saveEvent(true, EventState.PUBLISHED);
            saveComment(event, "Первый комментарий",
                    LocalDateTime.parse("2025-01-01 10:00:00", DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)));
            saveComment(event, "Второй комментарий",
                    LocalDateTime.parse("2025-01-02 10:00:00", DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)));


            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0)
                    .size(10)
                    .sort(Sort.by(Sort.Direction.DESC, "createdOn"))
                    .build();


            List<CommentDto> result = commentService.getCommentsForEvent(event.getId(), params);


            assertThat(result)
                    .hasSize(2)
                    .extracting(CommentDto::getText)
                    .containsExactly("Второй комментарий", "Первый комментарий"); // DESC-сортировка
        }

        @Test
        @DisplayName("Возвращает пустой список, если комментарии отключены")
        void shouldReturnEmptyList_whenCommentsDisabled() {

            Event event = saveEvent(false, EventState.PUBLISHED);
            saveComment(event, "Отключённый комментарий", LocalDateTime.now());

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0)
                    .size(10)
                    .sort(Sort.unsorted())
                    .build();

            List<CommentDto> result = commentService.getCommentsForEvent(event.getId(), params);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Бросает EntityNotFoundException, когда событие не опубликовано")
        void shouldThrowException_whenEventNotPublished() {

            Event event = saveEvent(true, EventState.CANCELED);

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0)
                    .size(10)
                    .sort(Sort.unsorted())
                    .build();

            assertThrows(EntityNotFoundException.class,
                    () -> commentService.getCommentsForEvent(event.getId(), params));
        }

        @Test
        @DisplayName("Корректная пагинация")
        void shouldApplyPagination() {

            Event event = saveEvent(true, EventState.PUBLISHED);
            for (int i = 0; i < 5; i++) {
                saveComment(event, "Комментарий " + i,
                        LocalDateTime.now().plusSeconds(i));
            }

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0)
                    .size(2)
                    .sort(Sort.by(Sort.Direction.ASC, "createdOn"))
                    .build();

            List<CommentDto> page1 = commentService.getCommentsForEvent(event.getId(), params);

            params = params.toBuilder().from(2).build();
            List<CommentDto> page2 = commentService.getCommentsForEvent(event.getId(), params);

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);

            params = params.toBuilder().from(4).build();
            List<CommentDto> page3 = commentService.getCommentsForEvent(event.getId(), params);
            assertThat(page3).hasSize(1);
        }

        @Test
        @DisplayName("Пустой список, когда у опубликованного события нет комментариев")
        void shouldReturnEmptyList_whenNoComments() {
            Event event = saveEvent(true, EventState.PUBLISHED);   // комментарии включены, но мы их не создаём

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0).size(10).sort(Sort.unsorted()).build();

            List<CommentDto> result = commentService.getCommentsForEvent(event.getId(), params);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Комментарий, помеченный как удалённый, не возвращается")
        void shouldIgnoreDeletedComments() {
            Event event = saveEvent(true, EventState.PUBLISHED);

            Comment deletedComment = Comment.builder()
                    .event(event)
                    .author(userRepository.save(
                            User.builder()
                                    .name("X")
                                    .email("x@example.com")
                                    .build()))
                    .text("Удалённый")
                    .createdOn(LocalDateTime.now())
                    .isDeleted(true)
                    .build();
            commentRepository.save(deletedComment);

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0).size(10).sort(Sort.unsorted()).build();

            List<CommentDto> result = commentService.getCommentsForEvent(event.getId(), params);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("EntityNotFoundException, когда событие не найдено")
        void shouldThrowException_whenEventNotFound() {
            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0).size(10).sort(Sort.unsorted()).build();

            assertThrows(EntityNotFoundException.class,
                    () -> commentService.getCommentsForEvent(9999L, params));
        }

    }

    /* ---------- Вспомогательные методы ---------- */
    private Event saveEvent(boolean commentsEnabled, EventState state) {


        Category category = categoryRepository.save(
                Category.builder()
                        .name("Тестовая категория")
                        .build());

        User initiator = userRepository.save(
                User.builder()
                        .name("Инициатор")
                        .email("initiator@example.com")
                        .build());

        Event event = Event.builder()
                .title("Событие")
                .annotation("Краткое описание события")
                .description("Описание")
                .state(state)
                .commentsEnabled(commentsEnabled)
                .category(category)
                .initiator(initiator)
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new Location(55.7522F, 37.6156F))
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .build();

        return eventRepository.save(event);
    }

    private void saveComment(Event event, String text, LocalDateTime createdOn) {

        User author = userRepository.save(
                User.builder()
                        .name("Автор")
                        .email("author_" + UUID.randomUUID() + "@example.com")
                        .build());

        Comment comment = Comment.builder()
                .event(event)
                .author(author)            // ← задаём автора
                .text(text)
                .createdOn(createdOn)
                .isDeleted(false)
                .build();

        commentRepository.save(comment);
    }
}