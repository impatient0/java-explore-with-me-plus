package ru.practicum.explorewithme.main.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UpdateCommentDto;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.service.CommentService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // <-- для post-запроса
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


@WebMvcTest(PrivateCommentController.class)
public class PrivateCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private ObjectMapper objectMapper;

    private final Long userId = 1L;
    private final Long eventId = 100L;

    private NewCommentDto newCommentDto;
    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        newCommentDto = NewCommentDto.builder()
                .text("Test comment text")
                .build();

        UserShortDto author = UserShortDto.builder()
                .id(2L)
                .name("testUser")
                .build();

        commentDto = CommentDto.builder()
                .id(10L)
                .text(newCommentDto.getText())
                .author(author)
                .eventId(eventId)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .isEdited(false)
                .build();
    }

    @Nested
    @DisplayName("Набор тестов для метода createComment")
    class CreateComment {

        @Test
        void createComment_whenValidInput_thenReturnsCreatedComment() throws Exception {
            when(commentService.addComment(eq(userId), eq(eventId), any(NewCommentDto.class)))
                    .thenReturn(commentDto);

            mockMvc.perform(post("/users/{userId}/comments?eventId={eventId}", userId, eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCommentDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(commentDto.getId()))
                    .andExpect(jsonPath("$.text").value(commentDto.getText()))
                    .andExpect(jsonPath("$.eventId").value(eventId))
                    .andExpect(jsonPath("$.author.id").value(commentDto.getAuthor().getId()))
                    .andExpect(jsonPath("$.author.name").value(commentDto.getAuthor().getName()))
                    .andExpect(jsonPath("$.isEdited").value(false));
        }

        @Test
        void createComment_whenInvalidText_thenReturnsBadRequest() throws Exception {
            NewCommentDto invalidDto = NewCommentDto.builder()
                    .text("")
                    .build();

            mockMvc.perform(post("/users/{userId}/comments?eventId={eventId}", userId, eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createComment_whenNegativeUserId_thenReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/users/{userId}/comments?eventId={eventId}", -1, eventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCommentDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void createComment_whenNegativeEventId_thenReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/users/{userId}/comments?eventId={eventId}", userId, -1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCommentDto)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Набор тестов для метода updateComment")
    class UpdateComment {

        @Test
        void updateComment_shouldReturnUpdatedComment_whenInputIsValid() throws Exception {
            Long commentId = commentDto.getId();

            UpdateCommentDto updateCommentDto = UpdateCommentDto.builder()
                    .text("Updated text")
                    .build();

            CommentDto updatedComment = CommentDto.builder()
                    .id(commentId)
                    .text(updateCommentDto.getText())
                    .author(commentDto.getAuthor())
                    .eventId(eventId)
                    .createdOn(commentDto.getCreatedOn())
                    .updatedOn(commentDto.getUpdatedOn())
                    .isEdited(true)
                    .build();

            when(commentService.updateUserComment(eq(userId), eq(commentId), any(UpdateCommentDto.class)))
                    .thenReturn(updatedComment);

            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCommentDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(commentId))
                    .andExpect(jsonPath("$.text").value(updateCommentDto.getText()))
                    .andExpect(jsonPath("$.author.id").value(commentDto.getAuthor().getId()))
                    .andExpect(jsonPath("$.isEdited").value(true));

            verify(commentService, times(1))
                    .updateUserComment(eq(userId), eq(commentId), any(UpdateCommentDto.class));
        }

        @Test
        void updateComment_shouldReturnBadRequest_whenPathVariablesInvalid() throws Exception {
            UpdateCommentDto updateCommentDto = UpdateCommentDto.builder()
                    .text("Comment text")
                    .build();

            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", -1, 10)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCommentDto)))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", 1, -10)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCommentDto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateComment_shouldReturnBadRequest_whenBodyTextBlank() throws Exception {
            UpdateCommentDto updateCommentDto = UpdateCommentDto.builder()
                    .text("   ")
                    .build();

            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentDto.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCommentDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").exists())
                    .andExpect(jsonPath("$.errors[0]").value("text: Comment text cannot be blank."));
        }

        @Test
        void updateComment_shouldReturnBadRequest_whenBodyTextTooLong() throws Exception {
            String longText = "a".repeat(2001);
            UpdateCommentDto updateCommentDto = UpdateCommentDto.builder()
                    .text(longText)
                    .build();

            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentDto.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCommentDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").exists())
                    .andExpect(jsonPath("$.errors[0]").value("text: Comment text must be between 1 and 2000 characters."));
        }

        @Test
        void updateComment_shouldReturnBadRequest_whenBodyEmpty() throws Exception {
            mockMvc.perform(patch("/users/{userId}/comments/{commentId}", userId, commentDto.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").exists())
                    .andExpect(jsonPath("$.errors[0]").value("text: Comment text cannot be blank."));
        }
    }
}
