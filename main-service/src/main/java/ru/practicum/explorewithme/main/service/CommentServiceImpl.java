package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.CommentMapper;
import ru.practicum.explorewithme.main.model.Category;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.model.Event;
import ru.practicum.explorewithme.main.repository.CommentRepository;
import ru.practicum.explorewithme.main.repository.EventRepository;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public List<CommentDto> getCommentsForEvent(Long eventId, PublicCommentParameters parameters) {

        Pageable pageable = PageRequest.of(parameters.getFrom() / parameters.getSize(),
                parameters.getSize());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event", "Id", eventId));

        if (!event.isCommentsEnabled()) {
            return List.of();
        }

        List<Comment> result = commentRepository.findByEventIdAndIsDeletedFalse(eventId, pageable).getContent().stream()
                .sorted(parameters.getSort().equals("createdOn,ASC") ?
                        Comparator.comparing(Comment::getCreatedOn) :
                        Comparator.comparing(Comment::getCreatedOn).reversed())
                .toList();

        return commentMapper.toDtoList(result);
    }
}
