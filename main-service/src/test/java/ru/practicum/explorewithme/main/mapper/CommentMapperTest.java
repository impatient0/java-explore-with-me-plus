package ru.practicum.explorewithme.main.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.explorewithme.main.dto.CommentAdminDto;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тесты для CommentMapper")
@ActiveProfiles("mapper_test")
@SpringBootTest
class CommentMapperTest {

    @Autowired
    private CommentMapper commentMapper;

    private User testAuthor;
    private Event testEvent;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        testAuthor = User.builder().id(1L).name("Comment Author").email("author@test.com").build();
        testEvent = Event.builder().id(100L).title("Test Event for Comments").build();
    }

    @Nested
    @DisplayName("Метод toDto (маппинг Comment -> CommentDto)")
    class ToDtoTests {

        @Test
        @DisplayName("Должен корректно маппить все поля Comment в CommentDto")
        void toDto_whenCommentIsValid_shouldMapAllFields() {
            Comment comment = Comment.builder()
                .id(1L)
                .text("This is a test comment.")
                .author(testAuthor)
                .event(testEvent)
                .createdOn(now.minusHours(1))
                .updatedOn(now)
                .isEdited(true)
                .isDeleted(false)
                .build();

            CommentDto dto = commentMapper.toDto(comment);

            assertNotNull(dto);
            assertEquals(comment.getId(), dto.getId());
            assertEquals(comment.getText(), dto.getText());
            assertEquals(comment.getCreatedOn(), dto.getCreatedOn());
            assertEquals(comment.getUpdatedOn(), dto.getUpdatedOn());
            assertEquals(comment.isEdited(), dto.getIsEdited());

            assertNotNull(dto.getAuthor());
            assertEquals(testAuthor.getId(), dto.getAuthor().getId());
            assertEquals(testAuthor.getName(), dto.getAuthor().getName());

            assertNotNull(dto.getEventId());
            assertEquals(testEvent.getId(), dto.getEventId());
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null Comment")
        void toDto_whenCommentIsNull_shouldReturnNull() {
            CommentDto dto = commentMapper.toDto(null);
            assertNull(dto);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать null для вложенных author и event в Comment")
        void toDto_whenNestedAuthorOrEventIsNull_shouldMapAccordingly() {
            Comment commentWithNullAuthor = Comment.builder()
                .id(2L)
                .text("Comment with null author")
                .author(null)
                .event(testEvent)
                .createdOn(now)
                .build();

            Comment commentWithNullEvent = Comment.builder()
                .id(3L)
                .text("Comment with null event")
                .author(testAuthor)
                .event(null)
                .createdOn(now)
                .build();

            CommentDto dtoWithNullAuthor = commentMapper.toDto(commentWithNullAuthor);
            CommentDto dtoWithNullEvent = commentMapper.toDto(commentWithNullEvent);

            assertNotNull(dtoWithNullAuthor);
            assertNull(dtoWithNullAuthor.getAuthor(), "Author DTO should be null if source author is null");
            assertNotNull(dtoWithNullAuthor.getEventId());

            assertNotNull(dtoWithNullEvent);
            assertNotNull(dtoWithNullEvent.getAuthor());
            assertNull(dtoWithNullEvent.getEventId(), "eventId should be null if source event is null (or handle as error)");
        }

        @Test
        @DisplayName("Должен корректно маппить, если updatedOn в Comment равен null")
        void toDto_whenUpdatedOnIsNull_shouldMapUpdatedOnAsNull() {
            Comment comment = Comment.builder()
                .id(4L)
                .text("Never updated comment")
                .author(testAuthor)
                .event(testEvent)
                .createdOn(now.minusDays(1))
                .updatedOn(null)
                .isEdited(false)
                .build();

            CommentDto dto = commentMapper.toDto(comment);

            assertNotNull(dto);
            assertNull(dto.getUpdatedOn());
            assertFalse(dto.getIsEdited());
        }
    }

    @Nested
    @DisplayName("Метод toComment (маппинг NewCommentDto -> Comment)")
    class ToCommentTests {

        @Test
        @DisplayName("Должен корректно маппить текст из NewCommentDto в Comment")
        void toComment_fromNewCommentDto_shouldMapText() {
            NewCommentDto newDto = NewCommentDto.builder().text("New comment text").build();

            Comment entity = commentMapper.toComment(newDto);

            assertNotNull(entity);
            assertEquals(newDto.getText(), entity.getText());

            assertNull(entity.getId());
            assertNull(entity.getCreatedOn());
            assertNull(entity.getUpdatedOn());
            assertNull(entity.getAuthor());
            assertNull(entity.getEvent());
            assertFalse(entity.isEdited());
            assertFalse(entity.isDeleted());
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null NewCommentDto")
        void toComment_whenNewCommentDtoIsNull_shouldReturnNull() {
            Comment entity = commentMapper.toComment(null);

            assertNull(entity);
        }
    }

    @Nested
    @DisplayName("Метод toDtoList (маппинг List<Comment> -> List<CommentDto>)")
    class ToDtoListTests {

        @Test
        @DisplayName("Должен корректно маппить список Comment в список CommentDto")
        void toDtoList_shouldMapListOfComments() {
            Comment comment1 = Comment.builder().id(1L).text("First").author(testAuthor).event(testEvent).createdOn(now).build();
            Comment comment2 = Comment.builder().id(2L).text("Second").author(testAuthor).event(testEvent).createdOn(now).build();
            List<Comment> comments = Arrays.asList(comment1, comment2);

            List<CommentDto> dtoList = commentMapper.toDtoList(comments);

            assertNotNull(dtoList);
            assertEquals(2, dtoList.size());
            assertEquals(comment1.getText(), dtoList.get(0).getText());
            assertEquals(comment2.getText(), dtoList.get(1).getText());
            assertEquals(testAuthor.getName(), dtoList.get(0).getAuthor().getName());
            assertEquals(testEvent.getId(), dtoList.get(1).getEventId());
        }

        @Test
        @DisplayName("Должен возвращать null, если на вход подан null список")
        void toDtoList_whenListIsNull_shouldReturnNull() {
            List<CommentDto> dtoList = commentMapper.toDtoList(null);

            assertNull(dtoList);
        }

        @Test
        @DisplayName("Должен возвращать пустой список, если на вход подан пустой список")
        void toDtoList_whenListIsEmpty_shouldReturnEmptyList() {
            List<CommentDto> dtoList = commentMapper.toDtoList(Collections.emptyList());

            assertNotNull(dtoList);
            assertTrue(dtoList.isEmpty());
        }
    }

    @Nested
    @DisplayName("Метод toAdminDto (маппинг Comment -> CommentAdminDto)")
    class ToAdminDtoTests {

        @Test
        @DisplayName("Должен корректно маппить все поля, включая isDeleted")
        void toAdminDto_shouldMapAllFieldsIncludingIsDeleted() {
            User authorModel = User.builder().id(1L).name("Admin Test Author").build();
            Category categoryModel = Category.builder().id(1L).name("Admin Test Category").build();
            Event eventModel = Event.builder().id(1L).category(categoryModel).initiator(authorModel).build();


            Comment comment = Comment.builder()
                .id(1L)
                .text("Admin DTO test comment")
                .author(authorModel)
                .event(eventModel)
                .createdOn(LocalDateTime.now().minusHours(1))
                .updatedOn(LocalDateTime.now())
                .isEdited(true)
                .isDeleted(true)
                .build();

            CommentAdminDto dto = commentMapper.toAdminDto(comment);

            assertNotNull(dto);
            assertEquals(comment.getId(), dto.getId());
            assertEquals(comment.getText(), dto.getText());
            assertEquals(comment.getCreatedOn(), dto.getCreatedOn());
            assertEquals(comment.getUpdatedOn(), dto.getUpdatedOn());
            assertEquals(comment.isEdited(), dto.getIsEdited());
            assertEquals(comment.isDeleted(), dto.getIsDeleted()); // Проверяем isDeleted

            assertNotNull(dto.getAuthor());
            assertEquals(authorModel.getId(), dto.getAuthor().getId());

            assertNotNull(dto.getEventId());
            assertEquals(eventModel.getId(), dto.getEventId());
        }

        @Test
        @DisplayName("Должен корректно маппить isDeleted=false")
        void toAdminDto_withIsDeletedFalse_shouldMapCorrectly() {
            User authorModel = User.builder().id(1L).name("Admin Test Author").build();
            Event eventModel = Event.builder().id(1L).build();

            Comment comment = Comment.builder()
                .id(2L)
                .text("Not deleted comment")
                .author(authorModel)
                .event(eventModel)
                .isDeleted(false)
                .build();

            CommentAdminDto dto = commentMapper.toAdminDto(comment);

            assertNotNull(dto);
            assertEquals(false, dto.getIsDeleted());
        }
    }
}