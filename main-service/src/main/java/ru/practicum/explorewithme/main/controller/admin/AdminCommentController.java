package ru.practicum.explorewithme.main.controller.admin;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.service.CommentService;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long commentId) {
        log.info("Admin: Received request to delete comment with Id: {}", commentId);
        commentService.deleteCommentByAdmin(commentId);
        log.info("Admin: Comment with Id: {} marked as deleted", commentId);
    }

    @PatchMapping("/{commentId}/restore")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto restoreComment(@PathVariable @Positive Long commentId) {
        log.info("Admin: Received request to restore comment with Id: {}", commentId);
        CommentDto restoredComment = commentService.restoreCommentByAdmin(commentId);
        log.info("Admin: Comment with Id: {} restored", commentId);
        return restoredComment;
    }
}