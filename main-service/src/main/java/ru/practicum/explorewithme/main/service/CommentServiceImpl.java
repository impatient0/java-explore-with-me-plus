package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    UserRepository userRepository;
    EventRepository eventRepository;
    CommentMapper commentMapper;
    CommentRepository commentRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new EntityNotFoundException("Пользователь с id " + userId + " не найден");
        }

        Optional<Event> event = eventRepository.findById(eventId);
        if (event.isEmpty()) {
            throw new EntityNotFoundException("Событие с id " + eventId + " не найден");
        }
        if (!event.get().getState().equals(EventState.PUBLISHED)) {
            throw new BusinessRuleViolationException("Событие еще не опубликовано");
        }
        if (!event.get().isCommentsEnabled()) {
            throw new BusinessRuleViolationException("Комментарии запрещены");
        }

        Comment comment = commentMapper.toComment(newCommentDto);

        comment.setAuthor(user.get());
        comment.setEvent(event.get());

        commentRepository.save(comment);

        return commentMapper.toDto(comment);
    }
}
