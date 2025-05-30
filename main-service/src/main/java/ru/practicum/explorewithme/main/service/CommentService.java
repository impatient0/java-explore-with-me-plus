package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;

public interface CommentService {

    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);
}
