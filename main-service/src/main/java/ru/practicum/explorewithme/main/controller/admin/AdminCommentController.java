package ru.practicum.explorewithme.main.controller.admin;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.service.CommentService;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;

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

    /**
     * Получение списка всех комментариев с возможностью фильтрации администратором.
     *
     * @param userId    ID автора комментария для фильтрации (опционально)
     * @param eventId   ID события для фильтрации (опционально)
     * @param isDeleted Фильтр по статусу удаления (true - удаленные, false - не удаленные, null - все) (опционально)
     * @param from      количество элементов, которые нужно пропустить для формирования текущего набора
     * @param size      количество элементов в наборе
     * @return Список CommentDto
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getAllCommentsAdmin(
        @RequestParam(name = "userId", required = false) @Positive Long userId,
        @RequestParam(name = "eventId", required = false) @Positive Long eventId,
        @RequestParam(name = "isDeleted", required = false) Boolean isDeleted,
        @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero int from,
        @RequestParam(name = "size", defaultValue = "10") @Positive int size) {

        log.info("Admin: Received request to get all comments with filters: userId={}, eventId={}, isDeleted={}, from={}, size={}",
            userId, eventId, isDeleted, from, size);

        AdminCommentSearchParams searchParams = AdminCommentSearchParams.builder()
            .userId(userId)
            .eventId(eventId)
            .isDeleted(isDeleted)
            .build();

        List<CommentDto> comments = commentService.getAllCommentsAdmin(searchParams, from, size);

        log.info("Admin: Found {} comments matching criteria.", comments.size());
        return comments;
    }
}