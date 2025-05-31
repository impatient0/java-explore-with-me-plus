package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams; // Новый импорт
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.util.List;

public interface CommentService {

    List<CommentDto> getCommentsForEvent(Long eventId, PublicCommentParameters publicCommentParameters);

    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    List<CommentDto> getAllCommentsAdmin(AdminCommentSearchParams searchParams, int from, int size);
}