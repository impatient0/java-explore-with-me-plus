package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.CommentMapper;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.model.EventState;
import ru.practicum.explorewithme.main.repository.CommentRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

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
}
