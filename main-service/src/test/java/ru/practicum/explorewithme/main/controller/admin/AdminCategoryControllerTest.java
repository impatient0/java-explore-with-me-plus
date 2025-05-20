package ru.practicum.explorewithme.main.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.main.dto.CategoryDto;
import ru.practicum.explorewithme.main.dto.NewCategoryDto;
import ru.practicum.explorewithme.main.error.EntityDeletedException;
import ru.practicum.explorewithme.main.error.EntityNotFoundException;
import ru.practicum.explorewithme.main.service.CategoryService;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import ru.practicum.explorewithme.main.error.EntityAlreadyExistsException;

@WebMvcTest(AdminCategoryController.class)
@DisplayName("Контроллер администрирования категорий должен")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private NewCategoryDto newCategoryDto;
    private CategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        newCategoryDto = new NewCategoryDto();
        newCategoryDto.setName("Тестовая категория");

        categoryDto = new CategoryDto();
        categoryDto.setId(1L);
        categoryDto.setName("Тестовая категория");
    }

    @Nested
    @DisplayName("при создании категории")
    class CreateCategoryTests {

        @Test
        @DisplayName("возвращать созданную категорию со статусом 201")
        void createCategory_ReturnsCreatedCategory() throws Exception {
            when(categoryService.createCategory(any(NewCategoryDto.class))).thenReturn(categoryDto);

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCategoryDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Тестовая категория")));

            verify(categoryService, times(1)).createCategory(any(NewCategoryDto.class));
        }

        @Test
        @DisplayName("возвращать 400 при создании категории с невалидными данными")
        void createCategory_WithInvalidData_ReturnsBadRequest() throws Exception {
            NewCategoryDto invalidRequest = new NewCategoryDto();

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any(NewCategoryDto.class));
        }

        @Test
        @DisplayName("возвращать корректный заголовок Content-Type в ответе")
        void createCategory_WithValidRequest_HasCorrectContentTypeHeader() throws Exception {
            when(categoryService.createCategory(any())).thenReturn(categoryDto);

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCategoryDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Content-Type", containsString(MediaType.APPLICATION_JSON_VALUE)))
                    .andExpect(jsonPath("$.id", is(1)));

            verify(categoryService, times(1)).createCategory(any());
        }

        @Test
        @DisplayName("возвращать 409 при попытке создания уже существующей категории")
        void createCategory_WithExistingName_ReturnsConflict() throws Exception {
            when(categoryService.createCategory(any(NewCategoryDto.class)))
                    .thenThrow(new EntityAlreadyExistsException("Category", "name", newCategoryDto.getName()));

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCategoryDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.reason", containsString("already exists")));

            verify(categoryService, times(1)).createCategory(any(NewCategoryDto.class));
        }

    }

    @Nested
    @DisplayName("при обновлении категории")
    class UpdateCategoryTests {

        @Test
        @DisplayName("возвращать обновленную категорию со статусом 200")
        void updateCategory_ReturnsUpdatedCategory() throws Exception {
            CategoryDto updatedCategoryDto = new CategoryDto();
            updatedCategoryDto.setId(1L);
            updatedCategoryDto.setName("Обновленная категория");

            when(categoryService.updateCategory(anyLong(), any(NewCategoryDto.class))).thenReturn(updatedCategoryDto);

            mockMvc.perform(patch("/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCategoryDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Обновленная категория")));

            verify(categoryService, times(1)).updateCategory(eq(1L), any(NewCategoryDto.class));
        }

        @Test
        @DisplayName("возвращать 404 при обновлении несуществующей категории")
        void updateCategory_WithNonExistingId_ReturnsNotFound() throws Exception {
            when(categoryService.updateCategory(anyLong(), any(NewCategoryDto.class)))
                    .thenThrow(new EntityNotFoundException("Category", "Id", 999L));

            mockMvc.perform(patch("/admin/categories/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newCategoryDto)))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).updateCategory(eq(999L), any(NewCategoryDto.class));
        }

        @Test
        @DisplayName("возвращать 400 при обновлении категории с невалидными данными")
        void updateCategory_WithInvalidData_ReturnsBadRequest() throws Exception {
            NewCategoryDto invalidRequest = new NewCategoryDto();
            invalidRequest.setName(""); // Пустое имя

            mockMvc.perform(patch("/admin/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).updateCategory(anyLong(), any(NewCategoryDto.class));
        }
    }

    @Nested
    @DisplayName("при удалении категории")
    class DeleteCategoryTests {

        @Test
        @DisplayName("возвращать статус 204 без тела ответа")
        void deleteCategory_ReturnsNoContent() throws Exception {
            doNothing().when(categoryService).deleteCategory(anyLong());

            mockMvc.perform(delete("/admin/categories/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(categoryService, times(1)).deleteCategory(1L);
        }

        @Test
        @DisplayName("возвращать 404 при удалении несуществующей категории")
        void deleteCategory_WithNonExistingId_ReturnsNotFound() throws Exception {
            doThrow(new EntityNotFoundException("Category", "Id", 999L))
                    .when(categoryService).deleteCategory(999L);

            mockMvc.perform(delete("/admin/categories/999"))
                    .andExpect(status().isNotFound());

            verify(categoryService, times(1)).deleteCategory(999L);
        }

        @Test
        @DisplayName("возвращать 409 при удалении категории, содержащей события")
        void deleteCategory_WithEvents_ReturnsConflict() throws Exception {
            doThrow(new EntityDeletedException("Category", "Id", 1L))
                    .when(categoryService).deleteCategory(1L);

            mockMvc.perform(delete("/admin/categories/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.reason", containsString("Restriction")));

            verify(categoryService, times(1)).deleteCategory(1L);
        }

    }

    @Nested
    @DisplayName("при обработке невалидного JSON")
    class InvalidJsonTests {

        @Test
        @DisplayName("возвращать 400 при синтаксически некорректном JSON")
        void request_WithInvalidJson_ReturnsBadRequest() throws Exception {
            String invalidJson = "{\"name\":\"Тестовая категория\","; // Незакрытая скобка

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.reason", containsString("Malformed JSON")));

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @DisplayName("возвращать 400 при некорректном JSON-массиве")
        void request_WithMalformedJsonArray_ReturnsBadRequest() throws Exception {
            String invalidJsonArray = "[{\"name\":\"Тестовая категория\",}]"; // Ошибка - лишняя запятая

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJsonArray))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any());
        }
    }

    @Nested
    @DisplayName("при проверке формата запросов и ответов")
    class RequestResponseFormatTests {

        @Test
        @DisplayName("обрабатывать запрос с пустым JSON-объектом")
        void handleEmptyJsonObject() throws Exception {
            String emptyJson = "{}";

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyJson))
                    .andExpect(status().isBadRequest());

            verify(categoryService, never()).createCategory(any());
        }

        @Test
        @DisplayName("корректно обрабатывать имена категорий со специальными символами")
        void handleCategoryNameWithSpecialCharacters() throws Exception {
            NewCategoryDto specialCharsDto = new NewCategoryDto();
            specialCharsDto.setName("Категория с !@#$%^&*()");

            CategoryDto responseDto = new CategoryDto();
            responseDto.setId(1L);
            responseDto.setName("Категория с !@#$%^&*()");

            when(categoryService.createCategory(any())).thenReturn(responseDto);

            mockMvc.perform(post("/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(specialCharsDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Категория с !@#$%^&*()")));
        }
    }
}