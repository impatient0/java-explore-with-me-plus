package ru.practicum.explorewithme.main.controller.priv;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.NewCommentDto;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.service.CommentService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
                .text("") // некорректный: пустая строка
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
