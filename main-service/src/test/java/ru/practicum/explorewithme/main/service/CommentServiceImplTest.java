package ru.practicum.explorewithme.main.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
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
    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        userId = 1L;
        eventId = 2L;
        user = new User();
        event = new Event();
    }

    @Test
    void addComment_success() {
        NewCommentDto newCommentDto = new NewCommentDto();
        event.setState(EventState.PUBLISHED);
        event.setCommentsEnabled(true);
        Comment comment = new Comment();
        CommentDto commentDto = new CommentDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(commentMapper.toComment(newCommentDto)).thenReturn(comment);
        when(commentMapper.toDto(comment)).thenReturn(commentDto);

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