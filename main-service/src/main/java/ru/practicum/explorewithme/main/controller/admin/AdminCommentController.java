package ru.practicum.explorewithme.main.controller.admin;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.service.CommentService;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

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
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "eventId", required = false) Long eventId,
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

    // TODO: DELETE /{commentId} (deleteCommentByAdmin)
    // TODO: PATCH /{commentId}/restore (restoreCommentByAdmin)
}