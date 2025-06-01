package ru.practicum.explorewithme.main.service;

import ru.practicum.explorewithme.main.dto.CommentAdminDto;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.util.List;

public interface CommentService {

    List<CommentDto> getCommentsForEvent(Long eventId, PublicCommentParameters publicCommentParameters);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateUserComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteCommentByAdmin(Long commentId);

    void deleteUserComment(Long userId, Long commentId);

    CommentAdminDto restoreCommentByAdmin(Long commentId);

    List<CommentAdminDto> getAllCommentsAdmin(AdminCommentSearchParams searchParams, int from, int size);
}