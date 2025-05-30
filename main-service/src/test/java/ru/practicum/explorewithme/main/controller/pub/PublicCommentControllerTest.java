package ru.practicum.explorewithme.main.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.service.CommentService;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.service.params.PublicCommentParameters;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(PublicCommentController.class)
@DisplayName("Публичный контроллер комментариев должен")
class PublicCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private CommentDto commentDto;
    private CommentDto anotherCommentDto;

    @BeforeEach
    void setUp() {
        UserShortDto firstAuthor  = UserShortDto.builder()
                .id(100L)
                .name("Автор 1")
                .build();

        UserShortDto secondAuthor = UserShortDto.builder()
                .id(101L)
                .name("Автор 2")
                .build();

        commentDto = CommentDto.builder()
                .id(1L)
                .text("Первый комментарий")
                .author(firstAuthor)
                .createdOn(LocalDateTime.now().minusDays(1))
                .build();

        anotherCommentDto = CommentDto.builder()
                .id(2L)
                .text("Второй комментарий")
                .author(secondAuthor)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("при получении списка комментариев события")
    class GetCommentsTests {

        @Test
        @DisplayName("возвращать список комментариев со статусом 200")
        void getComments_ReturnsList() throws Exception {
            long eventId = 5L;
            List<CommentDto> expected = List.of(commentDto, anotherCommentDto);

            when(commentService.getCommentsForEvent(eq(eventId), any(PublicCommentParameters.class)))
                .thenReturn(expected);

            mockMvc.perform(get("/events/{eventId}/comments", eventId)
                            .param("sort", "createdOn,DESC")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(commentDto.getId()))
                    .andExpect(jsonPath("$[1].id").value(anotherCommentDto.getId()));
        }

        @Test
        @DisplayName("возвращать пустой список, если комментариев нет")
        void getComments_WhenNone_ReturnsEmptyList() throws Exception {
            long eventId = 7L;

            when(commentService.getCommentsForEvent(eq(eventId), any(PublicCommentParameters.class)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events/{eventId}/comments", eventId)
                            .param("sort", "createdOn,DESC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("применять параметры пагинации и сортировки")
        void getComments_UsesPaginationAndSorting() throws Exception {
            long eventId = 11L;

            when(commentService.getCommentsForEvent(eq(eventId), any(PublicCommentParameters.class)))
                .thenReturn(List.of(commentDto));

            mockMvc.perform(get("/events/{eventId}/comments", eventId)
                            .param("from", "20")
                            .param("size", "5")
                            .param("sort", "createdOn,ASC"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("использовать значения по умолчанию, когда параметры не переданы")
        void getComments_DefaultParamsAreUsed() throws Exception {
            long eventId = 13L;

            when(commentService.getCommentsForEvent(eq(eventId), any(PublicCommentParameters.class)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/events/{eventId}/comments", eventId))
                    .andExpect(status().isOk());

            var captor = ArgumentCaptor.forClass(PublicCommentParameters.class);
            verify(commentService).getCommentsForEvent(eq(eventId), captor.capture());

            PublicCommentParameters params = captor.getValue();
            assertThat(params.getFrom()).isZero();
            assertThat(params.getSize()).isEqualTo(10);
            assertThat(params.getSort().toString()).isEqualTo("createdOn: DESC");
        }

        @Test
        @DisplayName("возвращать 400, если параметр sort не соответствует паттерну")
        void getComments_InvalidSort_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/events/{eventId}/comments", 1)
                            .param("sort", "wrong,value"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("возвращать 400 при отрицательном 'from'")
        void getComments_NegativeFrom_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/events/{eventId}/comments", 1)
                            .param("from", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("возвращать 400 при отрицательном 'size'")
        void getComments_NegativeSize_ReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/events/{eventId}/comments", 1)
                            .param("size", "-5"))
                    .andExpect(status().isBadRequest());
        }
    }
}