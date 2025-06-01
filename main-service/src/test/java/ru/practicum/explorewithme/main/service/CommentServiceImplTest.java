package ru.practicum.explorewithme.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
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
import ru.practicum.explorewithme.main.dto.CommentAdminDto;
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
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;

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

    @Captor
    private ArgumentCaptor<Comment> commentCaptor;
    @Captor
    private ArgumentCaptor<Predicate> predicateCaptor;

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
            when(commentRepository.saveAndFlush(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CommentDto result = commentService.updateUserComment(userId, commentId, updateCommentDto);

            Assertions.assertEquals("Updated text", result.getText());
            Assertions.assertTrue(comment.isEdited());
            verify(commentRepository).saveAndFlush(comment);
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

    @Nested
    @DisplayName("Набор тестов для метода getUserComments")
    class GetUserComments {

        @Test
        void getUserCommentsshouldReturnEmptyListwhenNoComments() {

            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findByAuthorIdAndIsDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(commentMapper.toDtoList(List.of())).thenReturn(List.of());

            List<CommentDto> result = commentService.getUserComments(userId, 0, 10);

            assertThat(result).isEmpty();

            verify(userRepository).existsById(userId);
            verify(commentRepository).findByAuthorIdAndIsDeletedFalse(eq(userId), any(Pageable.class));
            verify(commentMapper).toDtoList(List.of());
        }

        @Test
        void getUserCommentsshouldReturnCommentsDtoListwhenCommentsExist() {

            List<Comment> comments = List.of(comment);
            CommentDto commentDto = new CommentDto();
            List<CommentDto> commentDtos = List.of(commentDto);

            when(userRepository.existsById(userId)).thenReturn(true);
            when(commentRepository.findByAuthorIdAndIsDeletedFalse(eq(userId), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(comments));
            when(commentMapper.toDtoList(comments)).thenReturn(commentDtos);

            List<CommentDto> result = commentService.getUserComments(userId, 0, 10);

            assertThat(result).isEqualTo(commentDtos);

            verify(userRepository).existsById(userId);
            verify(commentRepository).findByAuthorIdAndIsDeletedFalse(eq(userId), any(Pageable.class));
            verify(commentMapper).toDtoList(comments);
        }

        @Test
        void getUserCommentsshouldThrowExceptionwhenUserNotFound() {

            when(userRepository.existsById(userId)).thenReturn(false);

            EntityNotFoundException exception = assertThrows(
                    EntityNotFoundException.class,
                    () -> commentService.getUserComments(userId, 0, 10)
            );

            assertThat(exception.getMessage()).contains("Пользователь с id " + userId + " не найден");
            verify(userRepository).existsById(userId);
            verifyNoInteractions(commentRepository, commentMapper);
        }
    }

    @Nested
    @DisplayName("Метод deleteCommentByAdmin")
    class DeleteCommentByAdminTests {
        private Comment existingComment;

        @BeforeEach
        void setUpDeleteAdmin() {
            existingComment = new Comment();
            existingComment.setId(commentId);
            existingComment.setDeleted(false);
        }

        @Test
        @DisplayName("Должен пометить комментарий как удаленный, если он не был удален")
        void deleteCommentByAdmin_whenNotDeleted_shouldMarkAsDeletedAndSave() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            commentService.deleteCommentByAdmin(commentId);

            verify(commentRepository).findById(commentId);
            verify(commentRepository).save(commentCaptor.capture());
            Comment savedComment = commentCaptor.getValue();
            assertTrue(savedComment.isDeleted());
        }

        @Test
        @DisplayName("Не должен вызывать save, если комментарий уже удален")
        void deleteCommentByAdmin_whenAlreadyDeleted_shouldDoNothing() {
            existingComment.setDeleted(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(existingComment));

            commentService.deleteCommentByAdmin(commentId);

            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если комментарий не найден")
        void deleteCommentByAdmin_whenCommentNotFound_shouldThrowEntityNotFoundException() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> commentService.deleteCommentByAdmin(commentId));
            assertTrue(ex.getMessage().contains("Comment with id=" + commentId + " not found"));
            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    @Nested
    @DisplayName("Метод deleteUserComment")
    class DeleteUserCommentTests {
        private Comment userComment;
        private final Long nonAuthorUserId = 999L;

        @BeforeEach
        void setUpDeleteUser() {
            userComment = new Comment();
            userComment.setId(commentId);
            userComment.setAuthor(user);
            userComment.setDeleted(false);
        }

        @Test
        @DisplayName("Пользователь должен успешно 'мягко' удалять свой комментарий")
        void deleteUserComment_whenUserIsAuthor_shouldMarkAsDeleted() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(userComment));
            when(commentRepository.save(any(Comment.class))).thenReturn(userComment);

            commentService.deleteUserComment(userId, commentId);

            verify(commentRepository).findById(commentId);
            verify(commentRepository).save(commentCaptor.capture());
            Comment savedComment = commentCaptor.getValue();
            assertTrue(savedComment.isDeleted());
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если пользователь не автор")
        void deleteUserComment_whenUserIsNotAuthor_shouldThrowException() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(userComment));

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> commentService.deleteUserComment(nonAuthorUserId, commentId));
            assertTrue(ex.getMessage().contains("not found for user"));

            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Не должен вызывать save, если комментарий уже удален пользователем")
        void deleteUserComment_whenAlreadyDeleted_shouldDoNothing() {
            userComment.setDeleted(true);
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(userComment));

            commentService.deleteUserComment(userId, commentId);

            verify(commentRepository).findById(commentId);
            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если комментарий не найден")
        void deleteUserComment_whenCommentNotFound_shouldThrowEntityNotFoundException() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> commentService.deleteUserComment(userId, commentId));
            assertTrue(ex.getMessage().contains("Comment with id=" + commentId + " not found"));
        }
    }

    @Nested
    @DisplayName("Метод restoreCommentByAdmin")
    class RestoreCommentByAdminTests {
        private Comment deletedComment;
        private Comment notDeletedComment;
        private CommentAdminDto mappedDto;

        @BeforeEach
        void setUpRestore() {
            deletedComment = new Comment();
            deletedComment.setId(commentId);
            deletedComment.setDeleted(true);
            deletedComment.setText("Some text");

            notDeletedComment = new Comment();
            notDeletedComment.setId(commentId + 1);
            notDeletedComment.setDeleted(false);
            notDeletedComment.setText("Another text");

            mappedDto = CommentAdminDto.builder().id(commentId).text("Some text").isEdited(false).build();
        }

        @Test
        @DisplayName("Должен восстанавливать удаленный комментарий и возвращать DTO")
        void restoreCommentByAdmin_whenCommentIsDeleted_shouldRestoreAndReturnDto() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(commentMapper.toAdminDto(any(Comment.class))).thenReturn(mappedDto);

            CommentAdminDto result = commentService.restoreCommentByAdmin(commentId);

            assertNotNull(result);
            assertEquals(mappedDto.getText(), result.getText());
            verify(commentRepository).findById(commentId);
            verify(commentRepository).save(commentCaptor.capture());
            Comment savedComment = commentCaptor.getValue();
            assertFalse(savedComment.isDeleted());
            verify(commentMapper).toAdminDto(savedComment);
        }

        @Test
        @DisplayName("Должен возвращать DTO без изменений, если комментарий не был удален")
        void restoreCommentByAdmin_whenCommentIsNotDeleted_shouldReturnDtoWithoutSaving() {
            when(commentRepository.findById(notDeletedComment.getId())).thenReturn(Optional.of(notDeletedComment));
            when(commentMapper.toAdminDto(notDeletedComment)).thenReturn(
                CommentAdminDto.builder().id(notDeletedComment.getId()).text(notDeletedComment.getText()).build()
            );

            CommentAdminDto result = commentService.restoreCommentByAdmin(notDeletedComment.getId());

            assertNotNull(result);
            assertEquals(notDeletedComment.getText(), result.getText());
            verify(commentRepository).findById(notDeletedComment.getId());
            verify(commentRepository, never()).save(any(Comment.class));
            verify(commentMapper).toAdminDto(notDeletedComment);
        }

        @Test
        @DisplayName("Должен выбросить EntityNotFoundException, если комментарий для восстановления не найден")
        void restoreCommentByAdmin_whenCommentNotFound_shouldThrowEntityNotFoundException() {
            when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> commentService.restoreCommentByAdmin(commentId));
            assertTrue(ex.getMessage().contains("Comment with id=" + commentId + " not found"));
        }
    }

    @Nested
    @DisplayName("Метод getAllCommentsAdmin")
    class GetAllCommentsAdminServiceTests {
        private Pageable defaultPageable;

        @BeforeEach
        void setUpAdminSearch() {
            defaultPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdOn"));
        }

        @Test
        @DisplayName("Должен вызывать commentRepository.findAll с предикатом и пагинацией")
        void getAllCommentsAdmin_withFilters_shouldCallRepositoryWithPredicate() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder()
                .userId(1L)
                .eventId(2L)
                .isDeleted(false)
                .build();
            Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);
            when(commentRepository.findAll(any(Predicate.class), eq(defaultPageable))).thenReturn(emptyPage);

            commentService.getAllCommentsAdmin(params, 0, 10);

            verify(commentRepository).findAll(predicateCaptor.capture(), eq(defaultPageable));
            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
            String predicateStr = capturedPredicate.toString();
            assertTrue(predicateStr.contains("comment.author.id = 1"));
            assertTrue(predicateStr.contains("comment.event.id = 2"));
            assertTrue(predicateStr.contains("comment.isDeleted = false"));
        }

        @Test
        @DisplayName("Должен вызывать commentRepository.findAll с пагинацией, если фильтры не заданы")
        void getAllCommentsAdmin_noFilters_shouldCallRepositoryWithoutPredicateParts() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().build();
            Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);
            when(commentRepository.findAll(any(Predicate.class), eq(defaultPageable))).thenReturn(emptyPage);

            commentService.getAllCommentsAdmin(params, 0, 10);

            verify(commentRepository).findAll(predicateCaptor.capture(), eq(defaultPageable));
            Predicate capturedPredicate = predicateCaptor.getValue();
            assertNotNull(capturedPredicate);
        }

        @Test
        @DisplayName("getAllCommentsAdmin БЕЗ ФИЛЬТРОВ: должен вызывать commentRepository.findAll(Pageable)")
        void getAllCommentsAdmin_noFiltersAtAll_shouldCallRepositoryFindAllWithPageable() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().build();
            Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);

            when(commentRepository.findAll(any(Predicate.class), eq(defaultPageable))).thenReturn(emptyPage);

            commentService.getAllCommentsAdmin(params, 0, 10);

            verify(commentRepository, times(1)).findAll(predicateCaptor.capture(), eq(defaultPageable));
            Predicate capturedPredicate = predicateCaptor.getValue();
            assertEquals(new BooleanBuilder(), capturedPredicate); // Проверяем, что предикат был пустым.
        }


        @Test
        @DisplayName("Должен возвращать пустой список, если репозиторий вернул пустую страницу")
        void getAllCommentsAdmin_whenRepositoryReturnsEmptyPage_shouldReturnEmptyList() {
            AdminCommentSearchParams params = AdminCommentSearchParams.builder().userId(1L).build();
            Page<Comment> emptyPage = new PageImpl<>(Collections.emptyList(), defaultPageable, 0);
            when(commentRepository.findAll(any(Predicate.class), eq(defaultPageable))).thenReturn(emptyPage);

            List<CommentAdminDto> result = commentService.getAllCommentsAdmin(params, 0, 10);

            verify(commentMapper, times(1)).toAdminDtoList(Collections.emptyList());
            assertTrue(result.isEmpty());
        }
    }
}