package ru.practicum.explorewithme.main.controller.priv;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.service.CommentService;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDto> createComment(
            @PathVariable @Positive Long userId,
            @RequestParam @Positive Long eventId,
            @Valid @RequestBody NewCommentDto newCommentDto) {

        log.info("Создание нового комментария {} зарегистрированным пользователем c id {} " +
                "к событию с id {}", newCommentDto, userId, eventId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(userId, eventId, newCommentDto));
    }

    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long commentId,
            @Valid @RequestBody UpdateCommentDto updateCommentDto) {

        log.info("Обновление комментария c id {} пользователем c id {}," +
                " новый комментарий {}", commentId, userId, updateCommentDto);

        return ResponseEntity.status(HttpStatus.OK).body(commentService.updateUserComment(userId, commentId, updateCommentDto));
    }
}
