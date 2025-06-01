package ru.practicum.explorewithme.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
import ru.practicum.explorewithme.main.dto.CommentAdminDto;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.model.Location;
import ru.practicum.explorewithme.main.model.User;
import ru.practicum.explorewithme.main.repository.CategoryRepository;
import ru.practicum.explorewithme.main.repository.CommentRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

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

    private Event event1, event2;
    private Comment comment1User1Event1, comment2User2Event1, comment3User1Event1Deleted, comment4User2Event2;
    private LocalDateTime now;
    private User user1, user2;
    private Category category1, category2;

    @BeforeEach
    void setUpEntities() {
        now = LocalDateTime.now();

        user1 = userRepository.save(
            User.builder().name("User One").email("user1@example.com").build());
        user2 = userRepository.save(
            User.builder().name("User Two").email("user2@example.com").build());
        category1 = categoryRepository.save(Category.builder().name("Первая категория").build());
        category2 = categoryRepository.save(Category.builder().name("Вторая категория").build());
        event1 = saveEvent(true, EventState.PUBLISHED, "Первое событие", user1,
            category1, now.plusDays(1));
        event2 = saveEvent(true, EventState.PUBLISHED, "Второе событие", user2,
            category2, now.plusDays(2));

        comment1User1Event1 = saveComment(event1, user1,
            "Первый комментарий", now.minusHours(3), false, false);
        comment2User2Event1 = saveComment(event1, user2,
            "Второй комментарий", now.minusHours(1), false, false);
        comment3User1Event1Deleted = saveComment(event1, user1,
            "Третий комментарий", now.minusHours(2), true,
            true);
        comment4User2Event2 = saveComment(event2, user2,
            "Четвёртый комментарий", now.plusSeconds(1), false,
            false);
    }

    @Nested
    @DisplayName("Метод getCommentsForEvent")
    class GetCommentsForEvent {

        @Test
        @DisplayName("Возвращает комментарии опубликованного события с включёнными комментариями")
        void shouldReturnComments_whenEventPublishedAndCommentsEnabled() {

            Event event = event1;

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

            Event event = saveEvent(false, EventState.PUBLISHED,
                "Событие с отключёнными комментариями", user2, category2, now.plusDays(11));
            saveComment(event2, user1, "Отключённый комментарий", now, false, false);

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

            Event event = saveEvent(true, EventState.CANCELED, "Отменённое событие", user1,
                category1, now.plusDays(12));

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

            Event event = event1;
            for (int i = 0; i < 3; i++) { // Добавляем ещё 3 комментария к 2 уже существующим.
                saveComment(event, user1, "Комментарий " + i, LocalDateTime.now().plusSeconds(i), false, false);
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
            Event event = saveEvent(true, EventState.PUBLISHED, "Событие без комментариев", user1, category2,
                now.plusDays(12));   // комментарии включены, но мы их не создаём

            PublicCommentParameters params = PublicCommentParameters.builder()
                    .from(0).size(10).sort(Sort.unsorted()).build();

            List<CommentDto> result = commentService.getCommentsForEvent(event.getId(), params);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Комментарий, помеченный как удалённый, не возвращается")
        void shouldIgnoreDeletedComments() {
            Event event = event2;

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

            assertThat(result).size().isEqualTo(1); // Новый комментарий не добавился
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

    @Nested
    @DisplayName("Метод getAllCommentsAdmin")
    class GetAllCommentsAdminIntegrationTests {

        @Test
        @DisplayName("Должен возвращать все комментарии (включая удаленные), если фильтры не указаны, с пагинацией")
        void getAllCommentsAdmin_noFilters_shouldReturnAllCommentsPaged() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().build();

            List<CommentAdminDto> resultsPage1 = commentService.getAllCommentsAdmin(params, 0, 2);
            List<CommentAdminDto> resultsPage2 = commentService.getAllCommentsAdmin(params, 2, 2);

            assertAll(
                () -> assertThat(resultsPage1)
                    .as("Страница 1: проверка количества комментариев")
                    .hasSize(2),
                () -> assertThat(resultsPage1.get(0))
                    .as("Страница 1, Комментарий 1: должен быть comment4User2Event2 (ID и Текст)")
                    .hasFieldOrPropertyWithValue("id", comment4User2Event2.getId())
                    .hasFieldOrPropertyWithValue("text", comment4User2Event2.getText()),
                () -> assertThat(resultsPage1.get(1))
                    .as("Страница 1, Комментарий 2: должен быть comment3User1Event1Deleted (ID и Текст)")
                    .hasFieldOrPropertyWithValue("id", comment3User1Event1Deleted.getId())
                    .hasFieldOrPropertyWithValue("text", comment3User1Event1Deleted.getText()),

                () -> assertThat(resultsPage2)
                    .as("Страница 2: проверка количества комментариев")
                    .hasSize(2),
                () -> assertThat(resultsPage2.get(0))
                    .as("Страница 2, Комментарий 1: должен быть comment2User2Event1 (ID и Текст)")
                    .hasFieldOrPropertyWithValue("id", comment2User2Event1.getId())
                    .hasFieldOrPropertyWithValue("text", comment2User2Event1.getText()),
                () -> assertThat(resultsPage2.get(1))
                    .as("Страница 2, Комментарий 2: должен быть comment1User1Event1 (ID и Текст)")
                    .hasFieldOrPropertyWithValue("id", comment1User1Event1.getId())
                    .hasFieldOrPropertyWithValue("text", comment1User1Event1.getText())
            );
        }

        @Test
        @DisplayName("Должен фильтровать по userId")
        void getAllCommentsAdmin_withUserIdFilter_shouldReturnUserComments() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder()
                .userId(user1.getId()).build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertAll(
                () -> assertThat(results)
                    .as("Должен вернуть 2 комментария для user1")
                    .hasSize(2),
                () -> assertThat(results)
                    .as("Все возвращенные комментарии должны принадлежать user1")
                    .allSatisfy(commentDto ->
                        assertThat(commentDto.getAuthor().getId())
                            .isEqualTo(user1.getId())),
                () -> assertThat(results)
                    .as("Комментарий 1 для user1 (ID: %s) должен присутствовать", comment1User1Event1.getId())
                    .extracting(CommentAdminDto::getId)
                    .contains(comment1User1Event1.getId()),
                () -> assertThat(results)
                    .as("Комментарий 2 для user1 (удален) (ID: %s) должен присутствовать", comment3User1Event1Deleted.getId())
                    .extracting(CommentAdminDto::getId)
                    .contains(comment3User1Event1Deleted.getId())
            );
        }

        @Test
        @DisplayName("Должен фильтровать по eventId")
        void getAllCommentsAdmin_withEventIdFilter_shouldReturnEventComments() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder()
                .eventId(event1.getId()).build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertAll(
                () -> assertThat(results)
                    .as("Должен вернуть 3 комментария для event1")
                    .hasSize(3),
                () -> assertThat(results)
                    .as("Все возвращенные комментарии должны принадлежать event1 (ID: %s)", event1.getId())
                    .allSatisfy(commentDto ->
                        assertThat(commentDto.getEventId())
                            .isEqualTo(event1.getId()))
            );
        }

        @Test
        @DisplayName("Должен фильтровать по isDeleted = false (только не удаленные)")
        void getAllCommentsAdmin_withIsDeletedFalse_shouldReturnNotDeletedComments() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().isDeleted(false)
                .build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertAll(
                () -> assertThat(results)
                    .as("Должен вернуть 3 неудаленных комментария")
                    .hasSize(3),
                () -> assertThat(results)
                    .as("Удаленный комментарий (comment2 ID: %s) НЕ должен присутствовать в результатах", comment3User1Event1Deleted.getId())
                    .extracting(CommentAdminDto::getId)
                    .doesNotContain(comment3User1Event1Deleted.getId()),
                () -> assertThat(results)
                    .as("Комментарий1 (ID: %s) должен присутствовать в результатах", comment1User1Event1.getId())
                    .extracting(CommentAdminDto::getId)
                    .contains(comment1User1Event1.getId()),
                () -> assertThat(results)
                    .as("Комментарий3 (ID: %s) должен присутствовать в результатах", comment2User2Event1.getId())
                    .extracting(CommentAdminDto::getId)
                    .contains(comment2User2Event1.getId()),
                () -> assertThat(results)
                    .as("Комментарий4 (ID: %s) должен присутствовать в результатах", comment4User2Event2.getId())
                    .extracting(CommentAdminDto::getId)
                    .contains(comment4User2Event2.getId()),
                () -> assertThat(results)
                    .as("Все полученные экземпляры CommentDto должны иметь ненулевой ID типа Long")
                    .allSatisfy(dto -> assertThat(dto.getId())
                        .as("ID для DTO с текстом '%s'", dto.getText())
                        .isNotNull()
                        .isInstanceOf(Long.class))
            );

            results.forEach(commentDto ->
                assertThat(findCommentInDb(commentDto.getId()).isDeleted())
                    .as("Комментарий %s (проверка БД) не должен быть помечен как удаленный", commentDto.getId())
                    .isFalse()
            );
        }

        @Test
        @DisplayName("Должен фильтровать по isDeleted = true (только удаленные)")
        void getAllCommentsAdmin_withIsDeletedTrue_shouldReturnDeletedComments() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().isDeleted(true)
                .build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertAll(
                () -> assertThat(results)
                    .as("Должен вернуть ровно 1 удаленный комментарий")
                    .hasSize(1),
                () -> assertThat(results.getFirst())
                    .as("Возвращенный комментарий должен быть comment2User1Event1Deleted (ID: %s)", comment3User1Event1Deleted.getId())
                    .hasFieldOrPropertyWithValue("id", comment3User1Event1Deleted.getId())
            );

            CommentAdminDto resultDto = results.getFirst();
            assertThat(findCommentInDb(resultDto.getId()).isDeleted())
                .as("Комментарий %s (проверка БД) должен быть помечен как удаленный", resultDto.getId())
                .isTrue();
        }

        @Test
        @DisplayName("Должен корректно работать с комбинацией фильтров (userId, eventId, isDeleted=false)")
        void getAllCommentsAdmin_withCombinedFilters_shouldReturnMatchingComments() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder()
                .userId(user1.getId()).eventId(event1.getId()).isDeleted(false).build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertAll(
                () -> assertThat(results)
                    .as("Должен вернуть ровно 1 комментарий по заданным фильтрам")
                    .hasSize(1),
                () -> {
                    CommentAdminDto resultDto = results.getFirst();
                    assertThat(resultDto)
                        .as("Возвращенный комментарий (ID: %s) должен соответствовать comment1User1Event1", resultDto.getId())
                        .hasFieldOrPropertyWithValue("id", comment1User1Event1.getId())
                        .hasFieldOrPropertyWithValue("text", comment1User1Event1.getText())
                        .hasFieldOrPropertyWithValue("eventId", event1.getId());
                    assertThat(resultDto)
                        .as("Автор возвращенного комментария (ID: %s) должен быть user1 (ID: %s)", resultDto.getId(), user1.getId())
                        .hasFieldOrPropertyWithValue("author.id", user1.getId());
                }
            );

            CommentAdminDto resultDtoForDbCheck = results.getFirst();
            assertThat(findCommentInDb(resultDtoForDbCheck.getId()).isDeleted())
                .as("Комментарий %s (проверка БД) не должен быть помечен как удаленный", resultDtoForDbCheck.getId())
                .isFalse();
        }

        @Test
        @DisplayName("Должен возвращать пустой список, если по фильтрам ничего не найдено")
        void getAllCommentsAdmin_whenNoMatch_shouldReturnEmptyList() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().userId(999L)
                .build();
            List<CommentAdminDto> results = commentService.getAllCommentsAdmin(params, 0, 10);

            assertThat(results).isEmpty();
        }
    }

    /* ---------- Вспомогательные методы ---------- */

    private Comment saveComment(Event event, User author, String text,
        LocalDateTime createdOn, boolean isEdited, boolean isDeleted) {
        Comment comment = Comment.builder().event(event).author(author).text(text)
            .createdOn(createdOn)
            .isEdited(isEdited).isDeleted(isDeleted).build();
        return commentRepository.saveAndFlush(
            comment);
    }

    private Event saveEvent(boolean commentsEnabled, EventState state, String title,
        User initiator, Category category, LocalDateTime eventDate) {
        Event event = Event.builder().title(title).annotation("Annotation for " + title)
            .description("Description for " + title).state(state)
            .commentsEnabled(commentsEnabled).category(category).initiator(initiator)
            .eventDate(eventDate).location(new Location(55.75f, 37.61f))
            .paid(false).participantLimit(0).requestModeration(true)
            .build();
        return eventRepository.saveAndFlush(event);
    }

    private Comment findCommentInDb(Long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new AssertionError(
            "Комментарий не найден в БД для проверки тестом: " + commentId));
    }
}