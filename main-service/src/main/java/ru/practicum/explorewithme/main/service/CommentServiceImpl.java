package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsForEvent(Long eventId, PublicCommentParameters parameters) {

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new EntityNotFoundException("Published event", "Id", eventId));

        if (!event.isCommentsEnabled()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(parameters.getFrom() / parameters.getSize(),
                parameters.getSize(), parameters.getSort());

        List<Comment> result = commentRepository.findByEventIdAndIsDeletedFalse(eventId, pageable).getContent();

        return commentMapper.toDtoList(result);
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new BusinessRuleViolationException("Событие еще не опубликовано");
        }

        if (!event.isCommentsEnabled()) {
            throw new BusinessRuleViolationException("Комментарии запрещены");
        }

        Comment comment = commentMapper.toComment(newCommentDto);

        comment.setAuthor(author);
        comment.setEvent(event);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {

        Optional<Comment> comment = commentRepository.findById(commentId);

        if (comment.isEmpty()) {
            throw new EntityNotFoundException("Комментарий с id" + commentId + " не найден");
        }

        Comment existedComment = comment.get();

        if (!existedComment.getAuthor().getId().equals(userId)) {
            throw new EntityNotFoundException("Искомый комментарий с id " + commentId + " пользователя с id " + userId + "не найден");
        }

        if (existedComment.isDeleted() == true) {
            throw new BusinessRuleViolationException("Редактирование невозможно. Комментарий удален");
        }

        if (existedComment.getCreatedOn().isBefore(LocalDateTime.now().minusHours(6))) {
            throw new BusinessRuleViolationException("Время для редактирования истекло");
        }

        existedComment.setText(updateCommentDto.getText());
        existedComment.setEdited(true);

        return commentMapper.toDto(commentRepository.save(existedComment));
    }
}
