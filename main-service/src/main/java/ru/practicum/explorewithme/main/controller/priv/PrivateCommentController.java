package ru.practicum.explorewithme.main.controller.priv;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.service.CommentService;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateCommentController {

    private final CommentService commentService;

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long commentId) {
        log.info("User id={}: Received request to delete comment with Id: {}", userId, commentId);
        commentService.deleteUserComment(userId, commentId);
        log.info("User id={}: Comment with Id: {} marked as deleted", userId, commentId);
    }
}