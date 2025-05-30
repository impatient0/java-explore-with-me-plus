package ru.practicum.explorewithme.main.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.mapper.CommentMapper;
import ru.practicum.explorewithme.main.model.Comment;
import ru.practicum.explorewithme.main.repository.CommentRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Comment with id=%d not found", commentId)));
        if (!comment.isDeleted()) {
            comment.setDeleted(true);
            commentRepository.save(comment);
        }
    }

    @Override
    @Transactional
    public void deleteUserComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Comment with id=%d not found", commentId)));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new EntityNotFoundException(String.format("Comment with id=%d not found for user with id=%d", commentId, userId));
        }
        if (!comment.isDeleted()) {
            comment.setDeleted(true);
            commentRepository.save(comment);
        }
    }

    @Override
    @Transactional
    public CommentDto restoreCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Comment with id=%d not found", commentId)));
        if (comment.isDeleted()) {
            comment.setDeleted(false);
            commentRepository.save(comment);
        }
        return commentMapper.toDto(comment);
    }
}