package ru.practicum.explorewithme.main.controller.admin;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested; // Добавлен импорт Nested
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CommentDto;
import ru.practicum.explorewithme.main.dto.UserShortDto;
import ru.practicum.explorewithme.main.service.CommentService;
import ru.practicum.explorewithme.main.service.params.AdminCommentSearchParams;

@WebMvcTest(AdminCommentController.class)
@DisplayName("Тесты для AdminCommentController")
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @Captor
    private ArgumentCaptor<AdminCommentSearchParams> paramsCaptor;

    @Nested
    @DisplayName("Метод GET /admin/comments")
    class GetAllCommentsAdminTests {

        @Test
        @DisplayName("Должен вернуть 200 OK и пустой список, если комментариев не найдено")
        void whenNoCommentsFound_shouldReturnOkAndEmptyList() throws Exception {
            when(commentService.getAllCommentsAdmin(any(AdminCommentSearchParams.class), eq(0), eq(10)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/comments")
                    .param("from", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

            verify(commentService).getAllCommentsAdmin(paramsCaptor.capture(), eq(0), eq(10));
            AdminCommentSearchParams capturedParams = paramsCaptor.getValue();
            assertNull(capturedParams.getUserId());
            assertNull(capturedParams.getEventId());
            assertNull(capturedParams.getIsDeleted());
        }

        @Test
        @DisplayName("Должен вернуть 200 OK и список CommentDto, если комментарии найдены")
        void whenCommentsFound_shouldReturnOkAndDtoList() throws Exception {
            UserShortDto author = UserShortDto.builder().id(1L).name("Test Author").build();
            CommentDto comment1 = CommentDto.builder().id(1L).text("Comment 1").author(author).eventId(100L).createdOn(LocalDateTime.now()).build();
            CommentDto comment2 = CommentDto.builder().id(2L).text("Comment 2").author(author).eventId(101L).createdOn(LocalDateTime.now()).build();
            List<CommentDto> comments = List.of(comment1, comment2);

            when(commentService.getAllCommentsAdmin(any(AdminCommentSearchParams.class), eq(0), eq(10)))
                .thenReturn(comments);

            mockMvc.perform(get("/admin/comments")
                    .param("from", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(comment1.getId().intValue())))
                .andExpect(jsonPath("$[0].text", is(comment1.getText())))
                .andExpect(jsonPath("$[1].id", is(comment2.getId().intValue())))
                .andExpect(jsonPath("$[1].text", is(comment2.getText())));

            verify(commentService).getAllCommentsAdmin(any(AdminCommentSearchParams.class), eq(0), eq(10));
        }

        @Test
        @DisplayName("Должен корректно передавать все параметры фильтрации в сервис")
        void withAllFilters_shouldPassFiltersToService() throws Exception {
            Long userIdFilter = 1L;
            Long eventIdFilter = 10L;
            Boolean isDeletedFilter = false;
            int from = 5;
            int size = 15;

            AdminCommentSearchParams expectedSearchParams = AdminCommentSearchParams.builder()
                .userId(userIdFilter)
                .eventId(eventIdFilter)
                .isDeleted(isDeletedFilter)
                .build();

            when(commentService.getAllCommentsAdmin(eq(expectedSearchParams), eq(from), eq(size)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/comments")
                    .param("userId", userIdFilter.toString())
                    .param("eventId", eventIdFilter.toString())
                    .param("isDeleted", isDeletedFilter.toString())
                    .param("from", String.valueOf(from))
                    .param("size", String.valueOf(size))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

            verify(commentService).getAllCommentsAdmin(eq(expectedSearchParams), eq(from), eq(size));
        }

        @Test
        @DisplayName("Должен использовать значения по умолчанию для from и size")
        void withDefaultPagination_shouldUseDefaultValues() throws Exception {
            AdminCommentSearchParams expectedSearchParams = AdminCommentSearchParams.builder().build();
            when(commentService.getAllCommentsAdmin(eq(expectedSearchParams), eq(0), eq(10)))
                .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/admin/comments")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

            verify(commentService).getAllCommentsAdmin(eq(expectedSearchParams), eq(0), eq(10));
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном 'from'")
        void withInvalidFrom_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/comments")
                    .param("from", "-1")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(commentService);
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном 'size'")
        void withInvalidSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/comments")
                    .param("from", "0")
                    .param("size", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(commentService);
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном 'userId'")
        void withInvalidUserIdFormat_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/comments")
                    .param("userId", "notANumber")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(commentService);
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном 'eventId'")
        void withInvalidEventIdFormat_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/comments")
                    .param("eventId", "notANumber")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(commentService);
        }

        @Test
        @DisplayName("Должен вернуть 400 Bad Request при невалидном 'isDeleted'")
        void withInvalidIsDeletedFormat_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/admin/comments")
                    .param("isDeleted", "notABoolean")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
            verifyNoInteractions(commentService);
        }
    }

    // TODO: тесты для DELETE /{commentId} (deleteCommentByAdmin)
    // TODO: тесты для PATCH /{commentId}/restore (restoreCommentByAdmin)
}