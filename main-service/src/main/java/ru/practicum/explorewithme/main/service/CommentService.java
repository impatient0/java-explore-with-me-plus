package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CommentDto;

public interface CommentService {
    void deleteCommentByAdmin(Long commentId);

    void deleteUserComment(Long userId, Long commentId);

    CommentDto restoreCommentByAdmin(Long commentId);
}