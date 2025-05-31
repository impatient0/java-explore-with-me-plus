package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.util.List;

public interface CommentService {

    List<CommentDto> getCommentsForEvent(Long eventId, PublicCommentParameters commentParameters);

    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);
}
