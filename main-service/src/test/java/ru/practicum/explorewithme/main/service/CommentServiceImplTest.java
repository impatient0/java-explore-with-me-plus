package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.error.BusinessRuleViolationException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.CommentMapper;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.model.User;
import ru.practicum.explorewithme.main.repository.CommentRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private long userId;
    private long eventId;
    private long commentId;
    private User user;
    private Event event;
    private Comment comment;

    @BeforeEach
    void setUp() {
        userId = 1L;
        eventId = 2L;
        commentId = 10L;
        user = new User();
        user.setId(userId);

        event = new Event();
        event.setId(eventId);

        comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(user);
        comment.setDeleted(false);
        comment.setEdited(false);
        comment.setText("Old text");
        comment.setCreatedOn(LocalDateTime.now().minusHours(5));
    }

    @Nested
    @DisplayName("Набор тестов для метода addComment")
    class AddComment {

        @Test
        void addComment_success() {
            NewCommentDto newCommentDto = new NewCommentDto();
            event.setState(EventState.PUBLISHED);
            event.setCommentsEnabled(true);

            CommentDto commentDto = new CommentDto();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(commentMapper.toComment(newCommentDto)).thenReturn(comment);
            when(commentRepository.save(any(Comment.class))).thenReturn(comment);
            when(commentMapper.toDto(any(Comment.class))).thenReturn(commentDto);

            CommentDto result = commentService.addComment(userId, eventId, newCommentDto);

            assertEquals(commentDto, result);
            verify(commentRepository, times(1)).save(comment);
            assertEquals(user, comment.getAuthor());
            assertEquals(event, comment.getEvent());
        }

        @Test
        void addComment_userNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> commentService.addComment(userId, 2L, new NewCommentDto()));
            assertTrue(ex.getMessage().contains("Пользователь с id " + userId + " не найден"));
        }

        @Test
        void addComment_eventNotFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> commentService.addComment(userId, eventId, new NewCommentDto()));
            assertTrue(ex.getMessage().contains("Событие с id " + eventId + " не найден"));
        }

        @Test
        void addComment_eventNotPublished() {
            event.setState(EventState.PENDING); // не опубликовано
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                    () -> commentService.addComment(userId, eventId, new NewCommentDto()));
            assertEquals("Событие еще не опубликовано", ex.getMessage());
        }

        @Test
        void addComment_commentsDisabled() {
            event.setState(EventState.PUBLISHED);
            event.setCommentsEnabled(false); // Комментарии запрещены
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                    () -> commentService.addComment(userId, eventId, new NewCommentDto()));
            assertEquals("Комментарии запрещены", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Набор тестов для метода updateUserComment")
    class UpdateUserComment {

        @Test
        void updateUserComment_shouldUpdateCommentAndReturnDto() {
            UpdateCommentDto updateCommentDto = new UpdateCommentDto();
            updateCommentDto.setText("Updated text");

            CommentDto expectedDto = new CommentDto();
            expectedDto.setId(commentId);
            expectedDto.setText("Updated text");

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            when(commentMapper.toDto(any(Comment.class))).thenReturn(expectedDto);
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CommentDto result = commentService.updateUserComment(userId, commentId, updateCommentDto);

            Assertions.assertEquals("Updated text", result.getText());
            Assertions.assertTrue(comment.isEdited());
            verify(commentRepository).save(comment);
        }

        @Test
        void updateUserComment_shouldThrowIfCommentNotFound() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
            UpdateCommentDto dto = new UpdateCommentDto();

            EntityNotFoundException ex = Assertions.assertThrows(
                    EntityNotFoundException.class,
                    () -> commentService.updateUserComment(userId, commentId, dto)
            );
            Assertions.assertTrue(ex.getMessage().contains("не найден"));
        }

        @Test
        void updateUserComment_shouldThrowIfUserIsNotAuthor() {
            User anotherUser = new User();
            anotherUser.setId(111L);
            comment.setAuthor(anotherUser);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            UpdateCommentDto dto = new UpdateCommentDto();

            EntityNotFoundException ex = Assertions.assertThrows(
                    EntityNotFoundException.class,
                    () -> commentService.updateUserComment(userId, commentId, dto)
            );
            Assertions.assertTrue(ex.getMessage().contains("пользователя с id"));
        }

        @Test
        void updateUserComment_shouldThrowIfDeleted() {
            comment.setDeleted(true);

            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            UpdateCommentDto dto = new UpdateCommentDto();

            BusinessRuleViolationException ex = Assertions.assertThrows(
                    BusinessRuleViolationException.class,
                    () -> commentService.updateUserComment(userId, commentId, dto)
            );
            Assertions.assertTrue(ex.getMessage().contains("удален"));
        }

        @Test
        void updateUserComment_shouldThrowIfTooLate() {

            comment.setCreatedOn(LocalDateTime.now().minusHours(7));
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
            UpdateCommentDto dto = new UpdateCommentDto();

            BusinessRuleViolationException ex = Assertions.assertThrows(
                    BusinessRuleViolationException.class,
                    () -> commentService.updateUserComment(userId, commentId, dto)
            );
            Assertions.assertTrue(ex.getMessage().contains("Время для редактирования истекло"));
        }
    }
}